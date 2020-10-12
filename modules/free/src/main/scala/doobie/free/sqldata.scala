// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import cats.~>
import cats.effect.{ Async, Cont, Fiber, Outcome, Poll, Sync }
import cats.effect.kernel.{ Deferred, Ref => CERef }
import cats.free.{ Free => FF } // alias because some algebras have an op called Free
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import com.github.ghik.silencer.silent

import java.lang.String
import java.sql.SQLData
import java.sql.SQLInput
import java.sql.SQLOutput

@silent("deprecated")
object sqldata { module =>

  // Algebra of operations for SQLData. Each accepts a visitor as an alternative to pattern-matching.
  sealed trait SQLDataOp[A] {
    def visit[F[_]](v: SQLDataOp.Visitor[F]): F[A]
  }

  // Free monad over SQLDataOp.
  type SQLDataIO[A] = FF[SQLDataOp, A]

  // Module of instances and constructors of SQLDataOp.
  object SQLDataOp {

    // Given a SQLData we can embed a SQLDataIO program in any algebra that understands embedding.
    implicit val SQLDataOpEmbeddable: Embeddable[SQLDataOp, SQLData] =
      new Embeddable[SQLDataOp, SQLData] {
        def embed[A](j: SQLData, fa: FF[SQLDataOp, A]) = Embedded.SQLData(j, fa)
      }

    // Interface for a natural transformation SQLDataOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (SQLDataOp ~> F) {
      final def apply[A](fa: SQLDataOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: SQLData => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def raiseError[A](e: Throwable): F[A]
      def handleErrorWith[A](fa: SQLDataIO[A])(f: Throwable => SQLDataIO[A]): F[A]
      def monotonic: F[FiniteDuration]
      def realTime: F[FiniteDuration]
      def delay[A](thunk: => A): F[A]
      def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
      def forceR[A, B](fa: SQLDataIO[A])(fb: SQLDataIO[B]): F[B]
      def uncancelable[A](body: Poll[SQLDataIO] => SQLDataIO[A]): F[A]
      def poll[A](poll: Any, fa: SQLDataIO[A]): F[A]
      def canceled: F[Unit]
      def onCancel[A](fa: SQLDataIO[A], fin: SQLDataIO[Unit]): F[A]
      def cede: F[Unit]
      def ref[A](a: A): F[CERef[SQLDataIO, A]]
      def deferred[A]: F[Deferred[SQLDataIO, A]]
      def sleep(time: FiniteDuration): F[Unit]
      def evalOn[A](fa: SQLDataIO[A], ec: ExecutionContext): F[A]
      def executionContext: F[ExecutionContext]
      def async[A](k: (Either[Throwable, A] => Unit) => SQLDataIO[Option[SQLDataIO[Unit]]]): F[A]

      // SQLData
      def getSQLTypeName: F[String]
      def readSQL(a: SQLInput, b: String): F[Unit]
      def writeSQL(a: SQLOutput): F[Unit]

    }

    // Common operations for all algebras.
    final case class Raw[A](f: SQLData => A) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    final case class Embed[A](e: Embedded[A]) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    final case class RaiseError[A](e: Throwable) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raiseError(e)
    }
    final case class HandleErrorWith[A](fa: SQLDataIO[A], f: Throwable => SQLDataIO[A]) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa)(f)
    }
    case object Monotonic extends SQLDataOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.monotonic
    }
    case object Realtime extends SQLDataOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.realTime
    }
    case class Suspend[A](hint: Sync.Type, thunk: () => A) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.suspend(hint)(thunk())
    }
    case class ForceR[A, B](fa: SQLDataIO[A], fb: SQLDataIO[B]) extends SQLDataOp[B] {
      def visit[F[_]](v: Visitor[F]) = v.forceR(fa)(fb)
    }
    case class Uncancelable[A](body: Poll[SQLDataIO] => SQLDataIO[A]) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.uncancelable(body)
    }
    case class Poll1[A](poll: Any, fa: SQLDataIO[A]) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.poll(poll, fa)
    }
    case object Canceled extends SQLDataOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.canceled
    }
    case class OnCancel[A](fa: SQLDataIO[A], fin: SQLDataIO[Unit]) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.onCancel(fa, fin)
    }
    case object Cede extends SQLDataOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.cede
    }
    case class Ref1[A](a: A) extends SQLDataOp[CERef[SQLDataIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.ref(a)
    }
    case class Deferred1[A]() extends SQLDataOp[Deferred[SQLDataIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.deferred
    }
    case class Sleep(time: FiniteDuration) extends SQLDataOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.sleep(time)
    }
    case class EvalOn[A](fa: SQLDataIO[A], ec: ExecutionContext) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.evalOn(fa, ec)
    }
    case object ExecutionContext1 extends SQLDataOp[ExecutionContext] {
      def visit[F[_]](v: Visitor[F]) = v.executionContext
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => SQLDataIO[Option[SQLDataIO[Unit]]]) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // SQLData-specific operations.
    final case object GetSQLTypeName extends SQLDataOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.getSQLTypeName
    }
    final case class  ReadSQL(a: SQLInput, b: String) extends SQLDataOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.readSQL(a, b)
    }
    final case class  WriteSQL(a: SQLOutput) extends SQLDataOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeSQL(a)
    }

  }
  import SQLDataOp._

  // Smart constructors for operations common to all algebras.
  val unit: SQLDataIO[Unit] = FF.pure[SQLDataOp, Unit](())
  def pure[A](a: A): SQLDataIO[A] = FF.pure[SQLDataOp, A](a)
  def raw[A](f: SQLData => A): SQLDataIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[SQLDataOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def raiseError[A](err: Throwable): SQLDataIO[A] = FF.liftF[SQLDataOp, A](RaiseError(err))
  def handleErrorWith[A](fa: SQLDataIO[A])(f: Throwable => SQLDataIO[A]): SQLDataIO[A] = FF.liftF[SQLDataOp, A](HandleErrorWith(fa, f))
  val monotonic = FF.liftF[SQLDataOp, FiniteDuration](Monotonic)
  val realtime = FF.liftF[SQLDataOp, FiniteDuration](Realtime)
  def delay[A](thunk: => A) = FF.liftF[SQLDataOp, A](Suspend(Sync.Type.Delay, () => thunk))
  def suspend[A](hint: Sync.Type)(thunk: => A) = FF.liftF[SQLDataOp, A](Suspend(hint, () => thunk))
  def forceR[A, B](fa: SQLDataIO[A])(fb: SQLDataIO[B]) = FF.liftF[SQLDataOp, B](ForceR(fa, fb))
  def uncancelable[A](body: Poll[SQLDataIO] => SQLDataIO[A]) = FF.liftF[SQLDataOp, A](Uncancelable(body))
  def capturePoll[M[_]](mpoll: Poll[M]) = new Poll[SQLDataIO] {
    def apply[A](fa: SQLDataIO[A]) = FF.liftF[SQLDataOp, A](Poll1(mpoll, fa))
  }
  val canceled = FF.liftF[SQLDataOp, Unit](Canceled)
  def onCancel[A](fa: SQLDataIO[A], fin: SQLDataIO[Unit]) = FF.liftF[SQLDataOp, A](OnCancel(fa, fin))
  val cede = FF.liftF[SQLDataOp, Unit](Cede)
  def ref[A](a: A) = FF.liftF[SQLDataOp, CERef[SQLDataIO, A]](Ref1(a))
  def deferred[A] = FF.liftF[SQLDataOp, Deferred[SQLDataIO, A]](Deferred1())
  def sleep(time: FiniteDuration) = FF.liftF[SQLDataOp, Unit](Sleep(time))
  def evalOn[A](fa: SQLDataIO[A], ec: ExecutionContext) = FF.liftF[SQLDataOp, A](EvalOn(fa, ec))
  val executionContext = FF.liftF[SQLDataOp, ExecutionContext](ExecutionContext1)
  def async[A](k: (Either[Throwable, A] => Unit) => SQLDataIO[Option[SQLDataIO[Unit]]]) = FF.liftF[SQLDataOp, A](Async1(k))

  // Smart constructors for SQLData-specific operations.
  val getSQLTypeName: SQLDataIO[String] = FF.liftF(GetSQLTypeName)
  def readSQL(a: SQLInput, b: String): SQLDataIO[Unit] = FF.liftF(ReadSQL(a, b))
  def writeSQL(a: SQLOutput): SQLDataIO[Unit] = FF.liftF(WriteSQL(a))

  // SQLDataIO is an Async
  implicit val AsyncSQLDataIO: Async[SQLDataIO] =
    new Async[SQLDataIO] {
      val asyncM = FF.catsFreeMonadForFree[SQLDataOp]
      override def pure[A](x: A): SQLDataIO[A] = asyncM.pure(x)
      override def flatMap[A, B](fa: SQLDataIO[A])(f: A => SQLDataIO[B]): SQLDataIO[B] = asyncM.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => SQLDataIO[Either[A, B]]): SQLDataIO[B] = asyncM.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): SQLDataIO[A] = module.raiseError(e)
      override def handleErrorWith[A](fa: SQLDataIO[A])(f: Throwable => SQLDataIO[A]): SQLDataIO[A] = module.handleErrorWith(fa)(f)
      override def monotonic: SQLDataIO[FiniteDuration] = module.monotonic
      override def realTime: SQLDataIO[FiniteDuration] = module.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A): SQLDataIO[A] = module.suspend(hint)(thunk)
      override def forceR[A, B](fa: SQLDataIO[A])(fb: SQLDataIO[B]): SQLDataIO[B] = module.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[SQLDataIO] => SQLDataIO[A]): SQLDataIO[A] = module.uncancelable(body)
      override def canceled: SQLDataIO[Unit] = module.canceled
      override def onCancel[A](fa: SQLDataIO[A], fin: SQLDataIO[Unit]): SQLDataIO[A] = module.onCancel(fa, fin)
      override def start[A](fa: SQLDataIO[A]): SQLDataIO[Fiber[SQLDataIO, Throwable, A]] = module.raiseError(new Exception("Unimplemented"))
      override def cede: SQLDataIO[Unit] = module.cede
      override def racePair[A, B](fa: SQLDataIO[A], fb: SQLDataIO[B]): SQLDataIO[Either[(Outcome[SQLDataIO, Throwable, A], Fiber[SQLDataIO, Throwable, B]), (Fiber[SQLDataIO, Throwable, A], Outcome[SQLDataIO, Throwable, B])]] = module.raiseError(new Exception("Unimplemented"))
      override def ref[A](a: A): SQLDataIO[CERef[SQLDataIO, A]] = module.ref(a)
      override def deferred[A]: SQLDataIO[Deferred[SQLDataIO, A]] = module.deferred
      override def sleep(time: FiniteDuration): SQLDataIO[Unit] = module.sleep(time)
      override def evalOn[A](fa: SQLDataIO[A], ec: ExecutionContext): SQLDataIO[A] = module.evalOn(fa, ec)
      override def executionContext: SQLDataIO[ExecutionContext] = module.executionContext
      override def async[A](k: (Either[Throwable, A] => Unit) => SQLDataIO[Option[SQLDataIO[Unit]]]) = module.async(k)
      override def cont[A](body: Cont[SQLDataIO, A]): SQLDataIO[A] = Async.defaultCont(body)(this)
    }

}

