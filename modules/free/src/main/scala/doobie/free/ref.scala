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
import java.sql.Ref
import java.util.Map

@silent("deprecated")
object ref { module =>

  // Algebra of operations for Ref. Each accepts a visitor as an alternative to pattern-matching.
  sealed trait RefOp[A] {
    def visit[F[_]](v: RefOp.Visitor[F]): F[A]
  }

  // Free monad over RefOp.
  type RefIO[A] = FF[RefOp, A]

  // Module of instances and constructors of RefOp.
  object RefOp {

    // Given a Ref we can embed a RefIO program in any algebra that understands embedding.
    implicit val RefOpEmbeddable: Embeddable[RefOp, Ref] =
      new Embeddable[RefOp, Ref] {
        def embed[A](j: Ref, fa: FF[RefOp, A]) = Embedded.Ref(j, fa)
      }

    // Interface for a natural transformation RefOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (RefOp ~> F) {
      final def apply[A](fa: RefOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: Ref => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def raiseError[A](e: Throwable): F[A]
      def handleErrorWith[A](fa: RefIO[A])(f: Throwable => RefIO[A]): F[A]
      def monotonic: F[FiniteDuration]
      def realTime: F[FiniteDuration]
      def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
      def forceR[A, B](fa: RefIO[A])(fb: RefIO[B]): F[B]
      def uncancelable[A](body: Poll[RefIO] => RefIO[A]): F[A]
      def poll[A](poll: Any, fa: RefIO[A]): F[A]
      def canceled: F[Unit]
      def onCancel[A](fa: RefIO[A], fin: RefIO[Unit]): F[A]
      def cede: F[Unit]
      def ref[A](a: A): F[CERef[RefIO, A]]
      def deferred[A]: F[Deferred[RefIO, A]]
      def sleep(time: FiniteDuration): F[Unit]
      def evalOn[A](fa: RefIO[A], ec: ExecutionContext): F[A]
      def executionContext: F[ExecutionContext]
      def async[A](k: (Either[Throwable, A] => Unit) => RefIO[Option[RefIO[Unit]]]): F[A]

      // Ref
      def getBaseTypeName: F[String]
      def getObject: F[AnyRef]
      def getObject(a: Map[String, Class[_]]): F[AnyRef]
      def setObject(a: AnyRef): F[Unit]

    }

    // Common operations for all algebras.
    final case class Raw[A](f: Ref => A) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    final case class Embed[A](e: Embedded[A]) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    final case class RaiseError[A](e: Throwable) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raiseError(e)
    }
    final case class HandleErrorWith[A](fa: RefIO[A], f: Throwable => RefIO[A]) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa)(f)
    }
    case object Monotonic extends RefOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.monotonic
    }
    case object Realtime extends RefOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.realTime
    }
    case class Suspend[A](hint: Sync.Type, thunk: () => A) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.suspend(hint)(thunk())
    }
    case class ForceR[A, B](fa: RefIO[A], fb: RefIO[B]) extends RefOp[B] {
      def visit[F[_]](v: Visitor[F]) = v.forceR(fa)(fb)
    }
    case class Uncancelable[A](body: Poll[RefIO] => RefIO[A]) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.uncancelable(body)
    }
    case class Poll1[A](poll: Any, fa: RefIO[A]) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.poll(poll, fa)
    }
    case object Canceled extends RefOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.canceled
    }
    case class OnCancel[A](fa: RefIO[A], fin: RefIO[Unit]) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.onCancel(fa, fin)
    }
    case object Cede extends RefOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.cede
    }
    case class Ref1[A](a: A) extends RefOp[CERef[RefIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.ref(a)
    }
    case class Deferred1[A]() extends RefOp[Deferred[RefIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.deferred
    }
    case class Sleep(time: FiniteDuration) extends RefOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.sleep(time)
    }
    case class EvalOn[A](fa: RefIO[A], ec: ExecutionContext) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.evalOn(fa, ec)
    }
    case object ExecutionContext1 extends RefOp[ExecutionContext] {
      def visit[F[_]](v: Visitor[F]) = v.executionContext
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => RefIO[Option[RefIO[Unit]]]) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // Ref-specific operations.
    final case object GetBaseTypeName extends RefOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.getBaseTypeName
    }
    final case object GetObject extends RefOp[AnyRef] {
      def visit[F[_]](v: Visitor[F]) = v.getObject
    }
    final case class  GetObject1(a: Map[String, Class[_]]) extends RefOp[AnyRef] {
      def visit[F[_]](v: Visitor[F]) = v.getObject(a)
    }
    final case class  SetObject(a: AnyRef) extends RefOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setObject(a)
    }

  }
  import RefOp._

  // Smart constructors for operations common to all algebras.
  val unit: RefIO[Unit] = FF.pure[RefOp, Unit](())
  def pure[A](a: A): RefIO[A] = FF.pure[RefOp, A](a)
  def raw[A](f: Ref => A): RefIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[RefOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def raiseError[A](err: Throwable): RefIO[A] = FF.liftF[RefOp, A](RaiseError(err))
  def handleErrorWith[A](fa: RefIO[A])(f: Throwable => RefIO[A]): RefIO[A] = FF.liftF[RefOp, A](HandleErrorWith(fa, f))
  val monotonic = FF.liftF[RefOp, FiniteDuration](Monotonic)
  val realtime = FF.liftF[RefOp, FiniteDuration](Realtime)
  def suspend[A](hint: Sync.Type)(thunk: => A) = FF.liftF[RefOp, A](Suspend(hint, () => thunk))
  def uncancelable[A](body: Poll[RefIO] => RefIO[A]) = FF.liftF[RefOp, A](Uncancelable(body))
  def capturePoll[M[_]](mpoll: Poll[M]): Poll[RefIO] = new Poll[RefIO] {
    def apply[A](fa: RefIO[A]) = FF.liftF[RefOp, A](Poll1(mpoll, fa))
  }
  def forceR[A, B](fa: RefIO[A])(fb: RefIO[B]) = FF.liftF[RefOp, B](ForceR(fa, fb))
  val canceled = FF.liftF[RefOp, Unit](Canceled)
  def onCancel[A](fa: RefIO[A], fin: RefIO[Unit]) = FF.liftF[RefOp, A](OnCancel(fa, fin))
  val cede = FF.liftF[RefOp, Unit](Cede)
  def ref[A](a: A) = FF.liftF[RefOp, CERef[RefIO, A]](Ref1(a))
  def deferred[A] = FF.liftF[RefOp, Deferred[RefIO, A]](Deferred1())
  def sleep(time: FiniteDuration) = FF.liftF[RefOp, Unit](Sleep(time))
  def evalOn[A](fa: RefIO[A], ec: ExecutionContext) = FF.liftF[RefOp, A](EvalOn(fa, ec))
  val executionContext = FF.liftF[RefOp, ExecutionContext](ExecutionContext1)
  def async[A](k: (Either[Throwable, A] => Unit) => RefIO[Option[RefIO[Unit]]]) = FF.liftF[RefOp, A](Async1(k))

  // Smart constructors for Ref-specific operations.
  val getBaseTypeName: RefIO[String] = FF.liftF(GetBaseTypeName)
  val getObject: RefIO[AnyRef] = FF.liftF(GetObject)
  def getObject(a: Map[String, Class[_]]): RefIO[AnyRef] = FF.liftF(GetObject1(a))
  def setObject(a: AnyRef): RefIO[Unit] = FF.liftF(SetObject(a))

  // RefIO is an Async
  implicit val AsyncRefIO: Async[RefIO] =
    new Async[RefIO] {
      val asyncM = FF.catsFreeMonadForFree[RefOp]
      override def pure[A](x: A): RefIO[A] = asyncM.pure(x)
      override def flatMap[A, B](fa: RefIO[A])(f: A => RefIO[B]): RefIO[B] = asyncM.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => RefIO[Either[A, B]]): RefIO[B] = asyncM.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): RefIO[A] = module.raiseError(e)
      override def handleErrorWith[A](fa: RefIO[A])(f: Throwable => RefIO[A]): RefIO[A] = module.handleErrorWith(fa)(f)
      override def monotonic: RefIO[FiniteDuration] = module.monotonic
      override def realTime: RefIO[FiniteDuration] = module.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A): RefIO[A] = module.suspend(hint)(thunk)
      override def forceR[A, B](fa: RefIO[A])(fb: RefIO[B]): RefIO[B] = module.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[RefIO] => RefIO[A]): RefIO[A] = module.uncancelable(body)
      override def canceled: RefIO[Unit] = module.canceled
      override def onCancel[A](fa: RefIO[A], fin: RefIO[Unit]): RefIO[A] = module.onCancel(fa, fin)
      override def start[A](fa: RefIO[A]): RefIO[Fiber[RefIO, Throwable, A]] = module.raiseError(new Exception("Unimplemented"))
      override def cede: RefIO[Unit] = module.cede
      override def racePair[A, B](fa: RefIO[A], fb: RefIO[B]): RefIO[Either[(Outcome[RefIO, Throwable, A], Fiber[RefIO, Throwable, B]), (Fiber[RefIO, Throwable, A], Outcome[RefIO, Throwable, B])]] = module.raiseError(new Exception("Unimplemented"))
      override def ref[A](a: A): RefIO[CERef[RefIO, A]] = module.ref(a)
      override def deferred[A]: RefIO[Deferred[RefIO, A]] = module.deferred
      override def sleep(time: FiniteDuration): RefIO[Unit] = module.sleep(time)
      override def evalOn[A](fa: RefIO[A], ec: ExecutionContext): RefIO[A] = module.evalOn(fa, ec)
      override def executionContext: RefIO[ExecutionContext] = module.executionContext
      override def async[A](k: (Either[Throwable, A] => Unit) => RefIO[Option[RefIO[Unit]]]) = module.async(k)
      override def cont[A](body: Cont[RefIO, A]): RefIO[A] = Async.defaultCont(body)(this)
    }

}

