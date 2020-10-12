// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.free

import cats.~>
import cats.effect.{ Async, Cont, Fiber, Outcome, Poll, Sync }
import cats.effect.kernel.{ Deferred, Ref => CERef }
import cats.free.{ Free => FF } // alias because some algebras have an op called Free
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import com.github.ghik.silencer.silent

import java.lang.Class
import java.lang.String
import java.sql.{ Array => SqlArray }
import java.util.Map
import org.postgresql.PGConnection
import org.postgresql.PGNotification
import org.postgresql.copy.{ CopyManager => PGCopyManager }
import org.postgresql.fastpath.{ Fastpath => PGFastpath }
import org.postgresql.jdbc.AutoSave
import org.postgresql.jdbc.PreferQueryMode
import org.postgresql.largeobject.LargeObjectManager
import org.postgresql.replication.PGReplicationConnection

@silent("deprecated")
object pgconnection { module =>

  // Algebra of operations for PGConnection. Each accepts a visitor as an alternative to pattern-matching.
  sealed trait PGConnectionOp[A] {
    def visit[F[_]](v: PGConnectionOp.Visitor[F]): F[A]
  }

  // Free monad over PGConnectionOp.
  type PGConnectionIO[A] = FF[PGConnectionOp, A]

  // Module of instances and constructors of PGConnectionOp.
  object PGConnectionOp {

    // Given a PGConnection we can embed a PGConnectionIO program in any algebra that understands embedding.
    implicit val PGConnectionOpEmbeddable: Embeddable[PGConnectionOp, PGConnection] =
      new Embeddable[PGConnectionOp, PGConnection] {
        def embed[A](j: PGConnection, fa: FF[PGConnectionOp, A]) = Embedded.PGConnection(j, fa)
      }

    // Interface for a natural transformation PGConnectionOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (PGConnectionOp ~> F) {
      final def apply[A](fa: PGConnectionOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: PGConnection => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def raiseError[A](e: Throwable): F[A]
      def handleErrorWith[A](fa: PGConnectionIO[A])(f: Throwable => PGConnectionIO[A]): F[A]
      def monotonic: F[FiniteDuration]
      def realTime: F[FiniteDuration]
      def delay[A](thunk: => A): F[A]
      def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
      def forceR[A, B](fa: PGConnectionIO[A])(fb: PGConnectionIO[B]): F[B]
      def uncancelable[A](body: Poll[PGConnectionIO] => PGConnectionIO[A]): F[A]
      def poll[A](poll: Any, fa: PGConnectionIO[A]): F[A]
      def canceled: F[Unit]
      def onCancel[A](fa: PGConnectionIO[A], fin: PGConnectionIO[Unit]): F[A]
      def cede: F[Unit]
      def ref[A](a: A): F[CERef[PGConnectionIO, A]]
      def deferred[A]: F[Deferred[PGConnectionIO, A]]
      def sleep(time: FiniteDuration): F[Unit]
      def evalOn[A](fa: PGConnectionIO[A], ec: ExecutionContext): F[A]
      def executionContext: F[ExecutionContext]
      def async[A](k: (Either[Throwable, A] => Unit) => PGConnectionIO[Option[PGConnectionIO[Unit]]]): F[A]

      // PGConnection
      def addDataType(a: String, b: Class[_ <: org.postgresql.util.PGobject]): F[Unit]
      def addDataType(a: String, b: String): F[Unit]
      def createArrayOf(a: String, b: AnyRef): F[SqlArray]
      def escapeIdentifier(a: String): F[String]
      def escapeLiteral(a: String): F[String]
      def getAutosave: F[AutoSave]
      def getBackendPID: F[Int]
      def getCopyAPI: F[PGCopyManager]
      def getDefaultFetchSize: F[Int]
      def getFastpathAPI: F[PGFastpath]
      def getLargeObjectAPI: F[LargeObjectManager]
      def getNotifications: F[Array[PGNotification]]
      def getNotifications(a: Int): F[Array[PGNotification]]
      def getParameterStatus(a: String): F[String]
      def getParameterStatuses: F[Map[String, String]]
      def getPreferQueryMode: F[PreferQueryMode]
      def getPrepareThreshold: F[Int]
      def getReplicationAPI: F[PGReplicationConnection]
      def setAutosave(a: AutoSave): F[Unit]
      def setDefaultFetchSize(a: Int): F[Unit]
      def setPrepareThreshold(a: Int): F[Unit]

    }

    // Common operations for all algebras.
    final case class Raw[A](f: PGConnection => A) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    final case class Embed[A](e: Embedded[A]) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    final case class RaiseError[A](e: Throwable) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raiseError(e)
    }
    final case class HandleErrorWith[A](fa: PGConnectionIO[A], f: Throwable => PGConnectionIO[A]) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa)(f)
    }
    case object Monotonic extends PGConnectionOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.monotonic
    }
    case object Realtime extends PGConnectionOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.realTime
    }
    case class Suspend[A](hint: Sync.Type, thunk: () => A) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.suspend(hint)(thunk())
    }
    case class ForceR[A, B](fa: PGConnectionIO[A], fb: PGConnectionIO[B]) extends PGConnectionOp[B] {
      def visit[F[_]](v: Visitor[F]) = v.forceR(fa)(fb)
    }
    case class Uncancelable[A](body: Poll[PGConnectionIO] => PGConnectionIO[A]) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.uncancelable(body)
    }
    case class Poll1[A](poll: Any, fa: PGConnectionIO[A]) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.poll(poll, fa)
    }
    case object Canceled extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.canceled
    }
    case class OnCancel[A](fa: PGConnectionIO[A], fin: PGConnectionIO[Unit]) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.onCancel(fa, fin)
    }
    case object Cede extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.cede
    }
    case class Ref1[A](a: A) extends PGConnectionOp[CERef[PGConnectionIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.ref(a)
    }
    case class Deferred1[A]() extends PGConnectionOp[Deferred[PGConnectionIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.deferred
    }
    case class Sleep(time: FiniteDuration) extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.sleep(time)
    }
    case class EvalOn[A](fa: PGConnectionIO[A], ec: ExecutionContext) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.evalOn(fa, ec)
    }
    case object ExecutionContext1 extends PGConnectionOp[ExecutionContext] {
      def visit[F[_]](v: Visitor[F]) = v.executionContext
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => PGConnectionIO[Option[PGConnectionIO[Unit]]]) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // PGConnection-specific operations.
    final case class  AddDataType(a: String, b: Class[_ <: org.postgresql.util.PGobject]) extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.addDataType(a, b)
    }
    final case class  AddDataType1(a: String, b: String) extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.addDataType(a, b)
    }
    final case class  CreateArrayOf(a: String, b: AnyRef) extends PGConnectionOp[SqlArray] {
      def visit[F[_]](v: Visitor[F]) = v.createArrayOf(a, b)
    }
    final case class  EscapeIdentifier(a: String) extends PGConnectionOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.escapeIdentifier(a)
    }
    final case class  EscapeLiteral(a: String) extends PGConnectionOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.escapeLiteral(a)
    }
    final case object GetAutosave extends PGConnectionOp[AutoSave] {
      def visit[F[_]](v: Visitor[F]) = v.getAutosave
    }
    final case object GetBackendPID extends PGConnectionOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getBackendPID
    }
    final case object GetCopyAPI extends PGConnectionOp[PGCopyManager] {
      def visit[F[_]](v: Visitor[F]) = v.getCopyAPI
    }
    final case object GetDefaultFetchSize extends PGConnectionOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getDefaultFetchSize
    }
    final case object GetFastpathAPI extends PGConnectionOp[PGFastpath] {
      def visit[F[_]](v: Visitor[F]) = v.getFastpathAPI
    }
    final case object GetLargeObjectAPI extends PGConnectionOp[LargeObjectManager] {
      def visit[F[_]](v: Visitor[F]) = v.getLargeObjectAPI
    }
    final case object GetNotifications extends PGConnectionOp[Array[PGNotification]] {
      def visit[F[_]](v: Visitor[F]) = v.getNotifications
    }
    final case class  GetNotifications1(a: Int) extends PGConnectionOp[Array[PGNotification]] {
      def visit[F[_]](v: Visitor[F]) = v.getNotifications(a)
    }
    final case class  GetParameterStatus(a: String) extends PGConnectionOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.getParameterStatus(a)
    }
    final case object GetParameterStatuses extends PGConnectionOp[Map[String, String]] {
      def visit[F[_]](v: Visitor[F]) = v.getParameterStatuses
    }
    final case object GetPreferQueryMode extends PGConnectionOp[PreferQueryMode] {
      def visit[F[_]](v: Visitor[F]) = v.getPreferQueryMode
    }
    final case object GetPrepareThreshold extends PGConnectionOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getPrepareThreshold
    }
    final case object GetReplicationAPI extends PGConnectionOp[PGReplicationConnection] {
      def visit[F[_]](v: Visitor[F]) = v.getReplicationAPI
    }
    final case class  SetAutosave(a: AutoSave) extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setAutosave(a)
    }
    final case class  SetDefaultFetchSize(a: Int) extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setDefaultFetchSize(a)
    }
    final case class  SetPrepareThreshold(a: Int) extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setPrepareThreshold(a)
    }

  }
  import PGConnectionOp._

  // Smart constructors for operations common to all algebras.
  val unit: PGConnectionIO[Unit] = FF.pure[PGConnectionOp, Unit](())
  def pure[A](a: A): PGConnectionIO[A] = FF.pure[PGConnectionOp, A](a)
  def raw[A](f: PGConnection => A): PGConnectionIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[PGConnectionOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def raiseError[A](err: Throwable): PGConnectionIO[A] = FF.liftF[PGConnectionOp, A](RaiseError(err))
  def handleErrorWith[A](fa: PGConnectionIO[A])(f: Throwable => PGConnectionIO[A]): PGConnectionIO[A] = FF.liftF[PGConnectionOp, A](HandleErrorWith(fa, f))
  val monotonic = FF.liftF[PGConnectionOp, FiniteDuration](Monotonic)
  val realtime = FF.liftF[PGConnectionOp, FiniteDuration](Realtime)
  def delay[A](thunk: => A) = FF.liftF[PGConnectionOp, A](Suspend(Sync.Type.Delay, () => thunk))
  def suspend[A](hint: Sync.Type)(thunk: => A) = FF.liftF[PGConnectionOp, A](Suspend(hint, () => thunk))
  def forceR[A, B](fa: PGConnectionIO[A])(fb: PGConnectionIO[B]) = FF.liftF[PGConnectionOp, B](ForceR(fa, fb))
  def uncancelable[A](body: Poll[PGConnectionIO] => PGConnectionIO[A]) = FF.liftF[PGConnectionOp, A](Uncancelable(body))
  def capturePoll[M[_]](mpoll: Poll[M]): Poll[PGConnectionIO] = new Poll[PGConnectionIO] {
    def apply[A](fa: PGConnectionIO[A]) = FF.liftF[PGConnectionOp, A](Poll1(mpoll, fa))
  }
  val canceled = FF.liftF[PGConnectionOp, Unit](Canceled)
  def onCancel[A](fa: PGConnectionIO[A], fin: PGConnectionIO[Unit]) = FF.liftF[PGConnectionOp, A](OnCancel(fa, fin))
  val cede = FF.liftF[PGConnectionOp, Unit](Cede)
  def ref[A](a: A) = FF.liftF[PGConnectionOp, CERef[PGConnectionIO, A]](Ref1(a))
  def deferred[A] = FF.liftF[PGConnectionOp, Deferred[PGConnectionIO, A]](Deferred1())
  def sleep(time: FiniteDuration) = FF.liftF[PGConnectionOp, Unit](Sleep(time))
  def evalOn[A](fa: PGConnectionIO[A], ec: ExecutionContext) = FF.liftF[PGConnectionOp, A](EvalOn(fa, ec))
  val executionContext = FF.liftF[PGConnectionOp, ExecutionContext](ExecutionContext1)
  def async[A](k: (Either[Throwable, A] => Unit) => PGConnectionIO[Option[PGConnectionIO[Unit]]]) = FF.liftF[PGConnectionOp, A](Async1(k))

  // Smart constructors for PGConnection-specific operations.
  def addDataType(a: String, b: Class[_ <: org.postgresql.util.PGobject]): PGConnectionIO[Unit] = FF.liftF(AddDataType(a, b))
  def addDataType(a: String, b: String): PGConnectionIO[Unit] = FF.liftF(AddDataType1(a, b))
  def createArrayOf(a: String, b: AnyRef): PGConnectionIO[SqlArray] = FF.liftF(CreateArrayOf(a, b))
  def escapeIdentifier(a: String): PGConnectionIO[String] = FF.liftF(EscapeIdentifier(a))
  def escapeLiteral(a: String): PGConnectionIO[String] = FF.liftF(EscapeLiteral(a))
  val getAutosave: PGConnectionIO[AutoSave] = FF.liftF(GetAutosave)
  val getBackendPID: PGConnectionIO[Int] = FF.liftF(GetBackendPID)
  val getCopyAPI: PGConnectionIO[PGCopyManager] = FF.liftF(GetCopyAPI)
  val getDefaultFetchSize: PGConnectionIO[Int] = FF.liftF(GetDefaultFetchSize)
  val getFastpathAPI: PGConnectionIO[PGFastpath] = FF.liftF(GetFastpathAPI)
  val getLargeObjectAPI: PGConnectionIO[LargeObjectManager] = FF.liftF(GetLargeObjectAPI)
  val getNotifications: PGConnectionIO[Array[PGNotification]] = FF.liftF(GetNotifications)
  def getNotifications(a: Int): PGConnectionIO[Array[PGNotification]] = FF.liftF(GetNotifications1(a))
  def getParameterStatus(a: String): PGConnectionIO[String] = FF.liftF(GetParameterStatus(a))
  val getParameterStatuses: PGConnectionIO[Map[String, String]] = FF.liftF(GetParameterStatuses)
  val getPreferQueryMode: PGConnectionIO[PreferQueryMode] = FF.liftF(GetPreferQueryMode)
  val getPrepareThreshold: PGConnectionIO[Int] = FF.liftF(GetPrepareThreshold)
  val getReplicationAPI: PGConnectionIO[PGReplicationConnection] = FF.liftF(GetReplicationAPI)
  def setAutosave(a: AutoSave): PGConnectionIO[Unit] = FF.liftF(SetAutosave(a))
  def setDefaultFetchSize(a: Int): PGConnectionIO[Unit] = FF.liftF(SetDefaultFetchSize(a))
  def setPrepareThreshold(a: Int): PGConnectionIO[Unit] = FF.liftF(SetPrepareThreshold(a))

  // PGConnectionIO is an Async
  implicit val AsyncPGConnectionIO: Async[PGConnectionIO] =
    new Async[PGConnectionIO] {
      val asyncM = FF.catsFreeMonadForFree[PGConnectionOp]
      override def pure[A](x: A): PGConnectionIO[A] = asyncM.pure(x)
      override def flatMap[A, B](fa: PGConnectionIO[A])(f: A => PGConnectionIO[B]): PGConnectionIO[B] = asyncM.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => PGConnectionIO[Either[A, B]]): PGConnectionIO[B] = asyncM.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): PGConnectionIO[A] = module.raiseError(e)
      override def handleErrorWith[A](fa: PGConnectionIO[A])(f: Throwable => PGConnectionIO[A]): PGConnectionIO[A] = module.handleErrorWith(fa)(f)
      override def monotonic: PGConnectionIO[FiniteDuration] = module.monotonic
      override def realTime: PGConnectionIO[FiniteDuration] = module.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A): PGConnectionIO[A] = module.suspend(hint)(thunk)
      override def forceR[A, B](fa: PGConnectionIO[A])(fb: PGConnectionIO[B]): PGConnectionIO[B] = module.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[PGConnectionIO] => PGConnectionIO[A]): PGConnectionIO[A] = module.raiseError(new Exception("Unimplemented"))
      override def canceled: PGConnectionIO[Unit] = module.canceled
      override def onCancel[A](fa: PGConnectionIO[A], fin: PGConnectionIO[Unit]): PGConnectionIO[A] = module.onCancel(fa, fin)
      override def start[A](fa: PGConnectionIO[A]): PGConnectionIO[Fiber[PGConnectionIO, Throwable, A]] = module.raiseError(new Exception("Unimplemented"))
      override def cede: PGConnectionIO[Unit] = module.cede
      override def racePair[A, B](fa: PGConnectionIO[A], fb: PGConnectionIO[B]): PGConnectionIO[Either[(Outcome[PGConnectionIO, Throwable, A], Fiber[PGConnectionIO, Throwable, B]), (Fiber[PGConnectionIO, Throwable, A], Outcome[PGConnectionIO, Throwable, B])]] = module.raiseError(new Exception("Unimplemented"))
      override def ref[A](a: A): PGConnectionIO[CERef[PGConnectionIO, A]] = module.raiseError(new Exception("Unimplemented"))
      override def deferred[A]: PGConnectionIO[Deferred[PGConnectionIO, A]] = module.raiseError(new Exception("Unimplemented"))
      override def sleep(time: FiniteDuration): PGConnectionIO[Unit] = module.sleep(time)
      override def evalOn[A](fa: PGConnectionIO[A], ec: ExecutionContext): PGConnectionIO[A] = module.evalOn(fa, ec)
      override def executionContext: PGConnectionIO[ExecutionContext] = module.executionContext
      override def async[A](k: (Either[Throwable, A] => Unit) => PGConnectionIO[Option[PGConnectionIO[Unit]]]) = module.async(k)
      override def cont[A](body: Cont[PGConnectionIO, A]): PGConnectionIO[A] = Async.defaultCont(body)(this)
    }

}

