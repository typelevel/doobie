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
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverPropertyInfo
import java.util.Properties
import java.util.logging.Logger

@silent("deprecated")
object driver { module =>

  // Algebra of operations for Driver. Each accepts a visitor as an alternative to pattern-matching.
  sealed trait DriverOp[A] {
    def visit[F[_]](v: DriverOp.Visitor[F]): F[A]
  }

  // Free monad over DriverOp.
  type DriverIO[A] = FF[DriverOp, A]

  // Module of instances and constructors of DriverOp.
  object DriverOp {

    // Given a Driver we can embed a DriverIO program in any algebra that understands embedding.
    implicit val DriverOpEmbeddable: Embeddable[DriverOp, Driver] =
      new Embeddable[DriverOp, Driver] {
        def embed[A](j: Driver, fa: FF[DriverOp, A]) = Embedded.Driver(j, fa)
      }

    // Interface for a natural transformation DriverOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (DriverOp ~> F) {
      final def apply[A](fa: DriverOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: Driver => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def raiseError[A](e: Throwable): F[A]
      def handleErrorWith[A](fa: DriverIO[A])(f: Throwable => DriverIO[A]): F[A]
      def monotonic: F[FiniteDuration]
      def realTime: F[FiniteDuration]
      def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
      def forceR[A, B](fa: DriverIO[A])(fb: DriverIO[B]): F[B]
      def uncancelable[A](body: Poll[DriverIO] => DriverIO[A]): F[A]
      def poll[A](poll: Any, fa: DriverIO[A]): F[A]
      def canceled: F[Unit]
      def onCancel[A](fa: DriverIO[A], fin: DriverIO[Unit]): F[A]
      def cede: F[Unit]
      def ref[A](a: A): F[CERef[DriverIO, A]]
      def deferred[A]: F[Deferred[DriverIO, A]]
      def sleep(time: FiniteDuration): F[Unit]
      def evalOn[A](fa: DriverIO[A], ec: ExecutionContext): F[A]
      def executionContext: F[ExecutionContext]
      def async[A](k: (Either[Throwable, A] => Unit) => DriverIO[Option[DriverIO[Unit]]]): F[A]


      // Driver
      def acceptsURL(a: String): F[Boolean]
      def connect(a: String, b: Properties): F[Connection]
      def getMajorVersion: F[Int]
      def getMinorVersion: F[Int]
      def getParentLogger: F[Logger]
      def getPropertyInfo(a: String, b: Properties): F[Array[DriverPropertyInfo]]
      def jdbcCompliant: F[Boolean]

    }

    // Common operations for all algebras.
    final case class Raw[A](f: Driver => A) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    final case class Embed[A](e: Embedded[A]) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    final case class RaiseError[A](e: Throwable) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raiseError(e)
    }
    final case class HandleErrorWith[A](fa: DriverIO[A], f: Throwable => DriverIO[A]) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa)(f)
    }
    case object Monotonic extends DriverOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.monotonic
    }
    case object Realtime extends DriverOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.realTime
    }
    case class Suspend[A](hint: Sync.Type, thunk: () => A) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.suspend(hint)(thunk())
    }
    case class ForceR[A, B](fa: DriverIO[A], fb: DriverIO[B]) extends DriverOp[B] {
      def visit[F[_]](v: Visitor[F]) = v.forceR(fa)(fb)
    }
    case class Uncancelable[A](body: Poll[DriverIO] => DriverIO[A]) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.uncancelable(body)
    }
    case class Poll1[A](poll: Any, fa: DriverIO[A]) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.poll(poll, fa)
    }
    case object Canceled extends DriverOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.canceled
    }
    case class OnCancel[A](fa: DriverIO[A], fin: DriverIO[Unit]) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.onCancel(fa, fin)
    }
    case object Cede extends DriverOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.cede
    }
    case class Ref1[A](a: A) extends DriverOp[CERef[DriverIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.ref(a)
    }
    case class Deferred1[A]() extends DriverOp[Deferred[DriverIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.deferred
    }
    case class Sleep(time: FiniteDuration) extends DriverOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.sleep(time)
    }
    case class EvalOn[A](fa: DriverIO[A], ec: ExecutionContext) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.evalOn(fa, ec)
    }
    case object ExecutionContext1 extends DriverOp[ExecutionContext] {
      def visit[F[_]](v: Visitor[F]) = v.executionContext
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => DriverIO[Option[DriverIO[Unit]]]) extends DriverOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // Driver-specific operations.
    final case class  AcceptsURL(a: String) extends DriverOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.acceptsURL(a)
    }
    final case class  Connect(a: String, b: Properties) extends DriverOp[Connection] {
      def visit[F[_]](v: Visitor[F]) = v.connect(a, b)
    }
    final case object GetMajorVersion extends DriverOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getMajorVersion
    }
    final case object GetMinorVersion extends DriverOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getMinorVersion
    }
    final case object GetParentLogger extends DriverOp[Logger] {
      def visit[F[_]](v: Visitor[F]) = v.getParentLogger
    }
    final case class  GetPropertyInfo(a: String, b: Properties) extends DriverOp[Array[DriverPropertyInfo]] {
      def visit[F[_]](v: Visitor[F]) = v.getPropertyInfo(a, b)
    }
    final case object JdbcCompliant extends DriverOp[Boolean] {
      def visit[F[_]](v: Visitor[F]) = v.jdbcCompliant
    }

  }
  import DriverOp._

  // Smart constructors for operations common to all algebras.
  val unit: DriverIO[Unit] = FF.pure[DriverOp, Unit](())
  def pure[A](a: A): DriverIO[A] = FF.pure[DriverOp, A](a)
  def raw[A](f: Driver => A): DriverIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[DriverOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def raiseError[A](err: Throwable): DriverIO[A] = FF.liftF[DriverOp, A](RaiseError(err))
  def handleErrorWith[A](fa: DriverIO[A])(f: Throwable => DriverIO[A]): DriverIO[A] = FF.liftF[DriverOp, A](HandleErrorWith(fa, f))
  val monotonic = FF.liftF[DriverOp, FiniteDuration](Monotonic)
  val realtime = FF.liftF[DriverOp, FiniteDuration](Realtime)
  def suspend[A](hint: Sync.Type)(thunk: => A) = FF.liftF[DriverOp, A](Suspend(hint, () => thunk))
  def forceR[A, B](fa: DriverIO[A])(fb: DriverIO[B]) = FF.liftF[DriverOp, B](ForceR(fa, fb))
  def uncancelable[A](body: Poll[DriverIO] => DriverIO[A]) = FF.liftF[DriverOp, A](Uncancelable(body))
  def capturePoll[M[_]](mpoll: Poll[M]): Poll[DriverIO] = new Poll[DriverIO] {
    def apply[A](fa: DriverIO[A]) = FF.liftF[DriverOp, A](Poll1(mpoll, fa))
  }
  val canceled = FF.liftF[DriverOp, Unit](Canceled)
  def onCancel[A](fa: DriverIO[A], fin: DriverIO[Unit]) = FF.liftF[DriverOp, A](OnCancel(fa, fin))
  val cede = FF.liftF[DriverOp, Unit](Cede)
  def ref[A](a: A) = FF.liftF[DriverOp, CERef[DriverIO, A]](Ref1(a))
  def deferred[A] = FF.liftF[DriverOp, Deferred[DriverIO, A]](Deferred1())
  def sleep(time: FiniteDuration) = FF.liftF[DriverOp, Unit](Sleep(time))
  def evalOn[A](fa: DriverIO[A], ec: ExecutionContext) = FF.liftF[DriverOp, A](EvalOn(fa, ec))
  val executionContext = FF.liftF[DriverOp, ExecutionContext](ExecutionContext1)
  def async[A](k: (Either[Throwable, A] => Unit) => DriverIO[Option[DriverIO[Unit]]]) = FF.liftF[DriverOp, A](Async1(k))

  // Smart constructors for Driver-specific operations.
  def acceptsURL(a: String): DriverIO[Boolean] = FF.liftF(AcceptsURL(a))
  def connect(a: String, b: Properties): DriverIO[Connection] = FF.liftF(Connect(a, b))
  val getMajorVersion: DriverIO[Int] = FF.liftF(GetMajorVersion)
  val getMinorVersion: DriverIO[Int] = FF.liftF(GetMinorVersion)
  val getParentLogger: DriverIO[Logger] = FF.liftF(GetParentLogger)
  def getPropertyInfo(a: String, b: Properties): DriverIO[Array[DriverPropertyInfo]] = FF.liftF(GetPropertyInfo(a, b))
  val jdbcCompliant: DriverIO[Boolean] = FF.liftF(JdbcCompliant)

  // DriverIO is an Async
  implicit val AsyncDriverIO: Async[DriverIO] =
    new Async[DriverIO] {
      val asyncM = FF.catsFreeMonadForFree[DriverOp]
      override def pure[A](x: A): DriverIO[A] = asyncM.pure(x)
      override def flatMap[A, B](fa: DriverIO[A])(f: A => DriverIO[B]): DriverIO[B] = asyncM.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => DriverIO[Either[A, B]]): DriverIO[B] = asyncM.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): DriverIO[A] = module.raiseError(e)
      override def handleErrorWith[A](fa: DriverIO[A])(f: Throwable => DriverIO[A]): DriverIO[A] = module.handleErrorWith(fa)(f)
      override def monotonic: DriverIO[FiniteDuration] = module.monotonic
      override def realTime: DriverIO[FiniteDuration] = module.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A): DriverIO[A] = module.suspend(hint)(thunk)
      override def forceR[A, B](fa: DriverIO[A])(fb: DriverIO[B]): DriverIO[B] = module.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[DriverIO] => DriverIO[A]): DriverIO[A] = module.uncancelable(body)
      override def canceled: DriverIO[Unit] = module.canceled
      override def onCancel[A](fa: DriverIO[A], fin: DriverIO[Unit]): DriverIO[A] = module.onCancel(fa, fin)
      override def start[A](fa: DriverIO[A]): DriverIO[Fiber[DriverIO, Throwable, A]] = module.raiseError(new Exception("Unimplemented"))
      override def cede: DriverIO[Unit] = module.cede
      override def racePair[A, B](fa: DriverIO[A], fb: DriverIO[B]): DriverIO[Either[(Outcome[DriverIO, Throwable, A], Fiber[DriverIO, Throwable, B]), (Fiber[DriverIO, Throwable, A], Outcome[DriverIO, Throwable, B])]] = module.raiseError(new Exception("Unimplemented"))
      override def ref[A](a: A): DriverIO[CERef[DriverIO, A]] = module.ref(a)
      override def deferred[A]: DriverIO[Deferred[DriverIO, A]] = module.deferred
      override def sleep(time: FiniteDuration): DriverIO[Unit] = module.sleep(time)
      override def evalOn[A](fa: DriverIO[A], ec: ExecutionContext): DriverIO[A] = module.evalOn(fa, ec)
      override def executionContext: DriverIO[ExecutionContext] = module.executionContext
      override def async[A](k: (Either[Throwable, A] => Unit) => DriverIO[Option[DriverIO[Unit]]]) = module.async(k)
      override def cont[A](body: Cont[DriverIO, A]): DriverIO[A] = Async.defaultCont(body)(this)
    }

}

