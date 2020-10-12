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

import java.lang.String
import java.sql.ResultSet
import org.postgresql.fastpath.FastpathArg
import org.postgresql.fastpath.{ Fastpath => PGFastpath }

@silent("deprecated")
object fastpath { module =>

  // Algebra of operations for PGFastpath. Each accepts a visitor as an alternative to pattern-matching.
  sealed trait FastpathOp[A] {
    def visit[F[_]](v: FastpathOp.Visitor[F]): F[A]
  }

  // Free monad over FastpathOp.
  type FastpathIO[A] = FF[FastpathOp, A]

  // Module of instances and constructors of FastpathOp.
  object FastpathOp {

    // Given a PGFastpath we can embed a FastpathIO program in any algebra that understands embedding.
    implicit val FastpathOpEmbeddable: Embeddable[FastpathOp, PGFastpath] =
      new Embeddable[FastpathOp, PGFastpath] {
        def embed[A](j: PGFastpath, fa: FF[FastpathOp, A]) = Embedded.Fastpath(j, fa)
      }

    // Interface for a natural transformation FastpathOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (FastpathOp ~> F) {
      final def apply[A](fa: FastpathOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: PGFastpath => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def raiseError[A](e: Throwable): F[A]
      def handleErrorWith[A](fa: FastpathIO[A])(f: Throwable => FastpathIO[A]): F[A]
      def monotonic: F[FiniteDuration]
      def realTime: F[FiniteDuration]
      def delay[A](thunk: => A): F[A]
      def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
      def forceR[A, B](fa: FastpathIO[A])(fb: FastpathIO[B]): F[B]
      def uncancelable[A](body: Poll[FastpathIO] => FastpathIO[A]): F[A]
      def poll[A](poll: Any, fa: FastpathIO[A]): F[A]
      def canceled: F[Unit]
      def onCancel[A](fa: FastpathIO[A], fin: FastpathIO[Unit]): F[A]
      def cede: F[Unit]
      def ref[A](a: A): F[CERef[FastpathIO, A]]
      def deferred[A]: F[Deferred[FastpathIO, A]]
      def sleep(time: FiniteDuration): F[Unit]
      def evalOn[A](fa: FastpathIO[A], ec: ExecutionContext): F[A]
      def executionContext: F[ExecutionContext]
      def async[A](k: (Either[Throwable, A] => Unit) => FastpathIO[Option[FastpathIO[Unit]]]): F[A]

      // PGFastpath
      def addFunction(a: String, b: Int): F[Unit]
      def addFunctions(a: ResultSet): F[Unit]
      def fastpath(a: Int, b: Array[FastpathArg]): F[Array[Byte]]
      def fastpath(a: Int, b: Boolean, c: Array[FastpathArg]): F[AnyRef]
      def fastpath(a: String, b: Array[FastpathArg]): F[Array[Byte]]
      def fastpath(a: String, b: Boolean, c: Array[FastpathArg]): F[AnyRef]
      def getData(a: String, b: Array[FastpathArg]): F[Array[Byte]]
      def getID(a: String): F[Int]
      def getInteger(a: String, b: Array[FastpathArg]): F[Int]
      def getLong(a: String, b: Array[FastpathArg]): F[Long]
      def getOID(a: String, b: Array[FastpathArg]): F[Long]

    }

    // Common operations for all algebras.
    final case class Raw[A](f: PGFastpath => A) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    final case class Embed[A](e: Embedded[A]) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    final case class RaiseError[A](e: Throwable) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raiseError(e)
    }
    final case class HandleErrorWith[A](fa: FastpathIO[A], f: Throwable => FastpathIO[A]) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa)(f)
    }
    case object Monotonic extends FastpathOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.monotonic
    }
    case object Realtime extends FastpathOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.realTime
    }
    case class Suspend[A](hint: Sync.Type, thunk: () => A) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.suspend(hint)(thunk())
    }
    case class ForceR[A, B](fa: FastpathIO[A], fb: FastpathIO[B]) extends FastpathOp[B] {
      def visit[F[_]](v: Visitor[F]) = v.forceR(fa)(fb)
    }
    case class Uncancelable[A](body: Poll[FastpathIO] => FastpathIO[A]) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.uncancelable(body)
    }
    case class Poll1[A](poll: Any, fa: FastpathIO[A]) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.poll(poll, fa)
    }
    case object Canceled extends FastpathOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.canceled
    }
    case class OnCancel[A](fa: FastpathIO[A], fin: FastpathIO[Unit]) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.onCancel(fa, fin)
    }
    case object Cede extends FastpathOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.cede
    }
    case class Ref1[A](a: A) extends FastpathOp[CERef[FastpathIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.ref(a)
    }
    case class Deferred1[A]() extends FastpathOp[Deferred[FastpathIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.deferred
    }
    case class Sleep(time: FiniteDuration) extends FastpathOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.sleep(time)
    }
    case class EvalOn[A](fa: FastpathIO[A], ec: ExecutionContext) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.evalOn(fa, ec)
    }
    case object ExecutionContext1 extends FastpathOp[ExecutionContext] {
      def visit[F[_]](v: Visitor[F]) = v.executionContext
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => FastpathIO[Option[FastpathIO[Unit]]]) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // PGFastpath-specific operations.
    final case class  AddFunction(a: String, b: Int) extends FastpathOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.addFunction(a, b)
    }
    final case class  AddFunctions(a: ResultSet) extends FastpathOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.addFunctions(a)
    }
    final case class  Fastpath(a: Int, b: Array[FastpathArg]) extends FastpathOp[Array[Byte]] {
      def visit[F[_]](v: Visitor[F]) = v.fastpath(a, b)
    }
    final case class  Fastpath1(a: Int, b: Boolean, c: Array[FastpathArg]) extends FastpathOp[AnyRef] {
      def visit[F[_]](v: Visitor[F]) = v.fastpath(a, b, c)
    }
    final case class  Fastpath2(a: String, b: Array[FastpathArg]) extends FastpathOp[Array[Byte]] {
      def visit[F[_]](v: Visitor[F]) = v.fastpath(a, b)
    }
    final case class  Fastpath3(a: String, b: Boolean, c: Array[FastpathArg]) extends FastpathOp[AnyRef] {
      def visit[F[_]](v: Visitor[F]) = v.fastpath(a, b, c)
    }
    final case class  GetData(a: String, b: Array[FastpathArg]) extends FastpathOp[Array[Byte]] {
      def visit[F[_]](v: Visitor[F]) = v.getData(a, b)
    }
    final case class  GetID(a: String) extends FastpathOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getID(a)
    }
    final case class  GetInteger(a: String, b: Array[FastpathArg]) extends FastpathOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getInteger(a, b)
    }
    final case class  GetLong(a: String, b: Array[FastpathArg]) extends FastpathOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.getLong(a, b)
    }
    final case class  GetOID(a: String, b: Array[FastpathArg]) extends FastpathOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.getOID(a, b)
    }

  }
  import FastpathOp._

  // Smart constructors for operations common to all algebras.
  val unit: FastpathIO[Unit] = FF.pure[FastpathOp, Unit](())
  def pure[A](a: A): FastpathIO[A] = FF.pure[FastpathOp, A](a)
  def raw[A](f: PGFastpath => A): FastpathIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[FastpathOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def raiseError[A](err: Throwable): FastpathIO[A] = FF.liftF[FastpathOp, A](RaiseError(err))
  def handleErrorWith[A](fa: FastpathIO[A])(f: Throwable => FastpathIO[A]): FastpathIO[A] = FF.liftF[FastpathOp, A](HandleErrorWith(fa, f))
  val monotonic = FF.liftF[FastpathOp, FiniteDuration](Monotonic)
  val realtime = FF.liftF[FastpathOp, FiniteDuration](Realtime)
  def delay[A](thunk: => A) = FF.liftF[FastpathOp, A](Suspend(Sync.Type.Delay, () => thunk))
  def suspend[A](hint: Sync.Type)(thunk: => A) = FF.liftF[FastpathOp, A](Suspend(hint, () => thunk))
  def forceR[A, B](fa: FastpathIO[A])(fb: FastpathIO[B]) = FF.liftF[FastpathOp, B](ForceR(fa, fb))
  def uncancelable[A](body: Poll[FastpathIO] => FastpathIO[A]) = FF.liftF[FastpathOp, A](Uncancelable(body))
  def capturePoll[M[_]](mpoll: Poll[M]): Poll[FastpathIO] = new Poll[FastpathIO] {
    def apply[A](fa: FastpathIO[A]) = FF.liftF[FastpathOp, A](Poll1(mpoll, fa))
  }
  val canceled = FF.liftF[FastpathOp, Unit](Canceled)
  def onCancel[A](fa: FastpathIO[A], fin: FastpathIO[Unit]) = FF.liftF[FastpathOp, A](OnCancel(fa, fin))
  val cede = FF.liftF[FastpathOp, Unit](Cede)
  def ref[A](a: A) = FF.liftF[FastpathOp, CERef[FastpathIO, A]](Ref1(a))
  def deferred[A] = FF.liftF[FastpathOp, Deferred[FastpathIO, A]](Deferred1())
  def sleep(time: FiniteDuration) = FF.liftF[FastpathOp, Unit](Sleep(time))
  def evalOn[A](fa: FastpathIO[A], ec: ExecutionContext) = FF.liftF[FastpathOp, A](EvalOn(fa, ec))
  val executionContext = FF.liftF[FastpathOp, ExecutionContext](ExecutionContext1)
  def async[A](k: (Either[Throwable, A] => Unit) => FastpathIO[Option[FastpathIO[Unit]]]) = FF.liftF[FastpathOp, A](Async1(k))

  // Smart constructors for Fastpath-specific operations.
  def addFunction(a: String, b: Int): FastpathIO[Unit] = FF.liftF(AddFunction(a, b))
  def addFunctions(a: ResultSet): FastpathIO[Unit] = FF.liftF(AddFunctions(a))
  def fastpath(a: Int, b: Array[FastpathArg]): FastpathIO[Array[Byte]] = FF.liftF(Fastpath(a, b))
  def fastpath(a: Int, b: Boolean, c: Array[FastpathArg]): FastpathIO[AnyRef] = FF.liftF(Fastpath1(a, b, c))
  def fastpath(a: String, b: Array[FastpathArg]): FastpathIO[Array[Byte]] = FF.liftF(Fastpath2(a, b))
  def fastpath(a: String, b: Boolean, c: Array[FastpathArg]): FastpathIO[AnyRef] = FF.liftF(Fastpath3(a, b, c))
  def getData(a: String, b: Array[FastpathArg]): FastpathIO[Array[Byte]] = FF.liftF(GetData(a, b))
  def getID(a: String): FastpathIO[Int] = FF.liftF(GetID(a))
  def getInteger(a: String, b: Array[FastpathArg]): FastpathIO[Int] = FF.liftF(GetInteger(a, b))
  def getLong(a: String, b: Array[FastpathArg]): FastpathIO[Long] = FF.liftF(GetLong(a, b))
  def getOID(a: String, b: Array[FastpathArg]): FastpathIO[Long] = FF.liftF(GetOID(a, b))

  // FastpathIO is an Async
  implicit val AsyncFastpathIO: Async[FastpathIO] =
    new Async[FastpathIO] {
      val asyncM = FF.catsFreeMonadForFree[FastpathOp]
      override def pure[A](x: A): FastpathIO[A] = asyncM.pure(x)
      override def flatMap[A, B](fa: FastpathIO[A])(f: A => FastpathIO[B]): FastpathIO[B] = asyncM.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => FastpathIO[Either[A, B]]): FastpathIO[B] = asyncM.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): FastpathIO[A] = module.raiseError(e)
      override def handleErrorWith[A](fa: FastpathIO[A])(f: Throwable => FastpathIO[A]): FastpathIO[A] = module.handleErrorWith(fa)(f)
      override def monotonic: FastpathIO[FiniteDuration] = module.monotonic
      override def realTime: FastpathIO[FiniteDuration] = module.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A): FastpathIO[A] = module.suspend(hint)(thunk)
      override def forceR[A, B](fa: FastpathIO[A])(fb: FastpathIO[B]): FastpathIO[B] = module.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[FastpathIO] => FastpathIO[A]): FastpathIO[A] = module.uncancelable(body)
      override def canceled: FastpathIO[Unit] = module.canceled
      override def onCancel[A](fa: FastpathIO[A], fin: FastpathIO[Unit]): FastpathIO[A] = module.onCancel(fa, fin)
      override def start[A](fa: FastpathIO[A]): FastpathIO[Fiber[FastpathIO, Throwable, A]] = module.raiseError(new Exception("Unimplemented"))
      override def cede: FastpathIO[Unit] = module.cede
      override def racePair[A, B](fa: FastpathIO[A], fb: FastpathIO[B]): FastpathIO[Either[(Outcome[FastpathIO, Throwable, A], Fiber[FastpathIO, Throwable, B]), (Fiber[FastpathIO, Throwable, A], Outcome[FastpathIO, Throwable, B])]] = module.raiseError(new Exception("Unimplemented"))
      override def ref[A](a: A): FastpathIO[CERef[FastpathIO, A]] = module.ref(a)
      override def deferred[A]: FastpathIO[Deferred[FastpathIO, A]] = module.deferred
      override def sleep(time: FiniteDuration): FastpathIO[Unit] = module.sleep(time)
      override def evalOn[A](fa: FastpathIO[A], ec: ExecutionContext): FastpathIO[A] = module.evalOn(fa, ec)
      override def executionContext: FastpathIO[ExecutionContext] = module.executionContext
      override def async[A](k: (Either[Throwable, A] => Unit) => FastpathIO[Option[FastpathIO[Unit]]]) = module.async(k)
      override def cont[A](body: Cont[FastpathIO, A]): FastpathIO[A] = Async.defaultCont(body)(this)
    }

}

