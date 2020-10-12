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

import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.lang.String
import java.sql.Clob
import java.sql.NClob

@silent("deprecated")
object nclob { module =>

  // Algebra of operations for NClob. Each accepts a visitor as an alternative to pattern-matching.
  sealed trait NClobOp[A] {
    def visit[F[_]](v: NClobOp.Visitor[F]): F[A]
  }

  // Free monad over NClobOp.
  type NClobIO[A] = FF[NClobOp, A]

  // Module of instances and constructors of NClobOp.
  object NClobOp {

    // Given a NClob we can embed a NClobIO program in any algebra that understands embedding.
    implicit val NClobOpEmbeddable: Embeddable[NClobOp, NClob] =
      new Embeddable[NClobOp, NClob] {
        def embed[A](j: NClob, fa: FF[NClobOp, A]) = Embedded.NClob(j, fa)
      }

    // Interface for a natural transformation NClobOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (NClobOp ~> F) {
      final def apply[A](fa: NClobOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: NClob => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def raiseError[A](e: Throwable): F[A]
      def handleErrorWith[A](fa: NClobIO[A])(f: Throwable => NClobIO[A]): F[A]
      def monotonic: F[FiniteDuration]
      def realTime: F[FiniteDuration]
      def delay[A](thunk: => A): F[A]
      def suspend[A](hint: Sync.Type)(thunk: => A): F[A]
      def forceR[A, B](fa: NClobIO[A])(fb: NClobIO[B]): F[B]
      def uncancelable[A](body: Poll[NClobIO] => NClobIO[A]): F[A]
      def poll[A](poll: Any, fa: NClobIO[A]): F[A]
      def canceled: F[Unit]
      def onCancel[A](fa: NClobIO[A], fin: NClobIO[Unit]): F[A]
      def cede: F[Unit]
      def ref[A](a: A): F[CERef[NClobIO, A]]
      def deferred[A]: F[Deferred[NClobIO, A]]
      def sleep(time: FiniteDuration): F[Unit]
      def evalOn[A](fa: NClobIO[A], ec: ExecutionContext): F[A]
      def executionContext: F[ExecutionContext]
      def async[A](k: (Either[Throwable, A] => Unit) => NClobIO[Option[NClobIO[Unit]]]): F[A]

      // NClob
      def free: F[Unit]
      def getAsciiStream: F[InputStream]
      def getCharacterStream: F[Reader]
      def getCharacterStream(a: Long, b: Long): F[Reader]
      def getSubString(a: Long, b: Int): F[String]
      def length: F[Long]
      def position(a: Clob, b: Long): F[Long]
      def position(a: String, b: Long): F[Long]
      def setAsciiStream(a: Long): F[OutputStream]
      def setCharacterStream(a: Long): F[Writer]
      def setString(a: Long, b: String): F[Int]
      def setString(a: Long, b: String, c: Int, d: Int): F[Int]
      def truncate(a: Long): F[Unit]

    }

    // Common operations for all algebras.
    final case class Raw[A](f: NClob => A) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    final case class Embed[A](e: Embedded[A]) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    final case class RaiseError[A](e: Throwable) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raiseError(e)
    }
    final case class HandleErrorWith[A](fa: NClobIO[A], f: Throwable => NClobIO[A]) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa)(f)
    }
    case object Monotonic extends NClobOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.monotonic
    }
    case object Realtime extends NClobOp[FiniteDuration] {
      def visit[F[_]](v: Visitor[F]) = v.realTime
    }
    case class Suspend[A](hint: Sync.Type, thunk: () => A) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.suspend(hint)(thunk())
    }
    case class ForceR[A, B](fa: NClobIO[A], fb: NClobIO[B]) extends NClobOp[B] {
      def visit[F[_]](v: Visitor[F]) = v.forceR(fa)(fb)
    }
    case class Uncancelable[A](body: Poll[NClobIO] => NClobIO[A]) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.uncancelable(body)
    }
    case class Poll1[A](poll: Any, fa: NClobIO[A]) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.poll(poll, fa)
    }
    case object Canceled extends NClobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.canceled
    }
    case class OnCancel[A](fa: NClobIO[A], fin: NClobIO[Unit]) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.onCancel(fa, fin)
    }
    case object Cede extends NClobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.cede
    }
    case class Ref1[A](a: A) extends NClobOp[CERef[NClobIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.ref(a)
    }
    case class Deferred1[A]() extends NClobOp[Deferred[NClobIO, A]] {
      def visit[F[_]](v: Visitor[F]) = v.deferred
    }
    case class Sleep(time: FiniteDuration) extends NClobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.sleep(time)
    }
    case class EvalOn[A](fa: NClobIO[A], ec: ExecutionContext) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.evalOn(fa, ec)
    }
    case object ExecutionContext1 extends NClobOp[ExecutionContext] {
      def visit[F[_]](v: Visitor[F]) = v.executionContext
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => NClobIO[Option[NClobIO[Unit]]]) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // NClob-specific operations.
    final case object Free extends NClobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.free
    }
    final case object GetAsciiStream extends NClobOp[InputStream] {
      def visit[F[_]](v: Visitor[F]) = v.getAsciiStream
    }
    final case object GetCharacterStream extends NClobOp[Reader] {
      def visit[F[_]](v: Visitor[F]) = v.getCharacterStream
    }
    final case class  GetCharacterStream1(a: Long, b: Long) extends NClobOp[Reader] {
      def visit[F[_]](v: Visitor[F]) = v.getCharacterStream(a, b)
    }
    final case class  GetSubString(a: Long, b: Int) extends NClobOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.getSubString(a, b)
    }
    final case object Length extends NClobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.length
    }
    final case class  Position(a: Clob, b: Long) extends NClobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.position(a, b)
    }
    final case class  Position1(a: String, b: Long) extends NClobOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.position(a, b)
    }
    final case class  SetAsciiStream(a: Long) extends NClobOp[OutputStream] {
      def visit[F[_]](v: Visitor[F]) = v.setAsciiStream(a)
    }
    final case class  SetCharacterStream(a: Long) extends NClobOp[Writer] {
      def visit[F[_]](v: Visitor[F]) = v.setCharacterStream(a)
    }
    final case class  SetString(a: Long, b: String) extends NClobOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.setString(a, b)
    }
    final case class  SetString1(a: Long, b: String, c: Int, d: Int) extends NClobOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.setString(a, b, c, d)
    }
    final case class  Truncate(a: Long) extends NClobOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.truncate(a)
    }

  }
  import NClobOp._

  // Smart constructors for operations common to all algebras.
  val unit: NClobIO[Unit] = FF.pure[NClobOp, Unit](())
  def pure[A](a: A): NClobIO[A] = FF.pure[NClobOp, A](a)
  def raw[A](f: NClob => A): NClobIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[NClobOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def raiseError[A](err: Throwable): NClobIO[A] = FF.liftF[NClobOp, A](RaiseError(err))
  def handleErrorWith[A](fa: NClobIO[A])(f: Throwable => NClobIO[A]): NClobIO[A] = FF.liftF[NClobOp, A](HandleErrorWith(fa, f))
  val monotonic = FF.liftF[NClobOp, FiniteDuration](Monotonic)
  val realtime = FF.liftF[NClobOp, FiniteDuration](Realtime)
  def delay[A](thunk: => A) = FF.liftF[NClobOp, A](Suspend(Sync.Type.Delay, () => thunk))
  def suspend[A](hint: Sync.Type)(thunk: => A) = FF.liftF[NClobOp, A](Suspend(hint, () => thunk))
  def forceR[A, B](fa: NClobIO[A])(fb: NClobIO[B]) = FF.liftF[NClobOp, B](ForceR(fa, fb))
  def uncancelable[A](body: Poll[NClobIO] => NClobIO[A]) = FF.liftF[NClobOp, A](Uncancelable(body))
  def capturePoll[M[_]](mpoll: Poll[M]) = new Poll[NClobIO] {
    def apply[A](fa: NClobIO[A]) = FF.liftF[NClobOp, A](Poll1(mpoll, fa))
  }
  val canceled = FF.liftF[NClobOp, Unit](Canceled)
  def onCancel[A](fa: NClobIO[A], fin: NClobIO[Unit]) = FF.liftF[NClobOp, A](OnCancel(fa, fin))
  val cede = FF.liftF[NClobOp, Unit](Cede)
  def ref[A](a: A) = FF.liftF[NClobOp, CERef[NClobIO, A]](Ref1(a))
  def deferred[A] = FF.liftF[NClobOp, Deferred[NClobIO, A]](Deferred1())
  def sleep(time: FiniteDuration) = FF.liftF[NClobOp, Unit](Sleep(time))
  def evalOn[A](fa: NClobIO[A], ec: ExecutionContext) = FF.liftF[NClobOp, A](EvalOn(fa, ec))
  val executionContext = FF.liftF[NClobOp, ExecutionContext](ExecutionContext1)
  def async[A](k: (Either[Throwable, A] => Unit) => NClobIO[Option[NClobIO[Unit]]]) = FF.liftF[NClobOp, A](Async1(k))

  // Smart constructors for NClob-specific operations.
  val free: NClobIO[Unit] = FF.liftF(Free)
  val getAsciiStream: NClobIO[InputStream] = FF.liftF(GetAsciiStream)
  val getCharacterStream: NClobIO[Reader] = FF.liftF(GetCharacterStream)
  def getCharacterStream(a: Long, b: Long): NClobIO[Reader] = FF.liftF(GetCharacterStream1(a, b))
  def getSubString(a: Long, b: Int): NClobIO[String] = FF.liftF(GetSubString(a, b))
  val length: NClobIO[Long] = FF.liftF(Length)
  def position(a: Clob, b: Long): NClobIO[Long] = FF.liftF(Position(a, b))
  def position(a: String, b: Long): NClobIO[Long] = FF.liftF(Position1(a, b))
  def setAsciiStream(a: Long): NClobIO[OutputStream] = FF.liftF(SetAsciiStream(a))
  def setCharacterStream(a: Long): NClobIO[Writer] = FF.liftF(SetCharacterStream(a))
  def setString(a: Long, b: String): NClobIO[Int] = FF.liftF(SetString(a, b))
  def setString(a: Long, b: String, c: Int, d: Int): NClobIO[Int] = FF.liftF(SetString1(a, b, c, d))
  def truncate(a: Long): NClobIO[Unit] = FF.liftF(Truncate(a))

  // NClobIO is an Async
  implicit val AsyncNClobIO: Async[NClobIO] =
    new Async[NClobIO] {
      val asyncM = FF.catsFreeMonadForFree[NClobOp]
      override def pure[A](x: A): NClobIO[A] = asyncM.pure(x)
      override def flatMap[A, B](fa: NClobIO[A])(f: A => NClobIO[B]): NClobIO[B] = asyncM.flatMap(fa)(f)
      override def tailRecM[A, B](a: A)(f: A => NClobIO[Either[A, B]]): NClobIO[B] = asyncM.tailRecM(a)(f)
      override def raiseError[A](e: Throwable): NClobIO[A] = module.raiseError(e)
      override def handleErrorWith[A](fa: NClobIO[A])(f: Throwable => NClobIO[A]): NClobIO[A] = module.handleErrorWith(fa)(f)
      override def monotonic: NClobIO[FiniteDuration] = module.monotonic
      override def realTime: NClobIO[FiniteDuration] = module.realtime
      override def suspend[A](hint: Sync.Type)(thunk: => A): NClobIO[A] = module.suspend(hint)(thunk)
      override def forceR[A, B](fa: NClobIO[A])(fb: NClobIO[B]): NClobIO[B] = module.forceR(fa)(fb)
      override def uncancelable[A](body: Poll[NClobIO] => NClobIO[A]): NClobIO[A] = module.uncancelable(body)
      override def canceled: NClobIO[Unit] = module.canceled
      override def onCancel[A](fa: NClobIO[A], fin: NClobIO[Unit]): NClobIO[A] = module.onCancel(fa, fin)
      override def start[A](fa: NClobIO[A]): NClobIO[Fiber[NClobIO, Throwable, A]] = module.raiseError(new Exception("Unimplemented"))
      override def cede: NClobIO[Unit] = module.cede
      override def racePair[A, B](fa: NClobIO[A], fb: NClobIO[B]): NClobIO[Either[(Outcome[NClobIO, Throwable, A], Fiber[NClobIO, Throwable, B]), (Fiber[NClobIO, Throwable, A], Outcome[NClobIO, Throwable, B])]] = module.raiseError(new Exception("Unimplemented"))
      override def ref[A](a: A): NClobIO[CERef[NClobIO, A]] = module.ref(a)
      override def deferred[A]: NClobIO[Deferred[NClobIO, A]] = module.deferred
      override def sleep(time: FiniteDuration): NClobIO[Unit] = module.sleep(time)
      override def evalOn[A](fa: NClobIO[A], ec: ExecutionContext): NClobIO[A] = module.evalOn(fa, ec)
      override def executionContext: NClobIO[ExecutionContext] = module.executionContext
      override def async[A](k: (Either[Throwable, A] => Unit) => NClobIO[Option[NClobIO[Unit]]]) = module.async(k)
      override def cont[A](body: Cont[NClobIO, A]): NClobIO[A] = Async.defaultCont(body)(this)
    }

}

