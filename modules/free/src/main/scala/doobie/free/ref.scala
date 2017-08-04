package doobie.free

import cats.~>
import cats.free.{ Free => FF }
import scala.util.{ Either => \/ }
import fs2.util.{ Catchable, Suspendable }

import java.lang.String
import java.sql.Ref
import java.util.Map

object ref {

  // Algebra of operations for Ref. Each accepts a visitor as an alternatie to pattern-matching.
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

    // Interface for a natural tansformation RefOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (RefOp ~> F) {
      final def apply[A](fa: RefOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: Ref => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def delay[A](a: () => A): F[A]
      def attempt[A](fa: RefIO[A]): F[Throwable \/ A]

      // Ref
      def getBaseTypeName: F[String]
      def getObject: F[AnyRef]
      def getObject(a: Map[String, Class[_]]): F[AnyRef]
      def setObject(a: AnyRef): F[Unit]

    }

    // Common operations for all algebras.
    case class Raw[A](f: Ref => A) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    case class Embed[A](e: Embedded[A]) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    case class  Delay[A](a: () => A) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    case class  Attempt[A](fa: RefIO[A]) extends RefOp[Throwable \/ A] {
      def visit[F[_]](v: Visitor[F]) = v.attempt(fa)
    }

    // Ref-specific operations.
    case object GetBaseTypeName extends RefOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.getBaseTypeName
    }
    case object GetObject extends RefOp[AnyRef] {
      def visit[F[_]](v: Visitor[F]) = v.getObject
    }
    case class  GetObject1(a: Map[String, Class[_]]) extends RefOp[AnyRef] {
      def visit[F[_]](v: Visitor[F]) = v.getObject(a)
    }
    case class  SetObject(a: AnyRef) extends RefOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setObject(a)
    }

  }
  import RefOp._

  // Smart constructors for operations common to all algebras.
  val unit: RefIO[Unit] = FF.pure[RefOp, Unit](())
  def raw[A](f: Ref => A): RefIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[RefOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def lift[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[RefOp, A] = embed(j, fa)
  def delay[A](a: => A): RefIO[A] = FF.liftF(Delay(() => a))
  def attempt[A](fa: RefIO[A]): RefIO[Throwable \/ A] = FF.liftF[RefOp, Throwable \/ A](Attempt(fa))
  def fail[A](err: Throwable): RefIO[A] = delay(throw err)

  // Smart constructors for Ref-specific operations.
  val getBaseTypeName: RefIO[String] = FF.liftF(GetBaseTypeName)
  val getObject: RefIO[AnyRef] = FF.liftF(GetObject)
  def getObject(a: Map[String, Class[_]]): RefIO[AnyRef] = FF.liftF(GetObject1(a))
  def setObject(a: AnyRef): RefIO[Unit] = FF.liftF(SetObject(a))

// RefIO can capture side-effects, and can trap and raise exceptions.
  implicit val CatchableRefIO: Suspendable[RefIO] with Catchable[RefIO] =
    new Suspendable[RefIO] with Catchable[RefIO] {
      def pure[A](a: A): RefIO[A] = ref.delay(a)
      override def map[A, B](fa: RefIO[A])(f: A => B): RefIO[B] = fa.map(f)
      def flatMap[A, B](fa: RefIO[A])(f: A => RefIO[B]): RefIO[B] = fa.flatMap(f)
      def suspend[A](fa: => RefIO[A]): RefIO[A] = FF.suspend(fa)
      override def delay[A](a: => A): RefIO[A] = ref.delay(a)
      def attempt[A](f: RefIO[A]): RefIO[Throwable \/ A] = ref.attempt(f)
      def fail[A](err: Throwable): RefIO[A] = ref.fail(err)
    }

}

