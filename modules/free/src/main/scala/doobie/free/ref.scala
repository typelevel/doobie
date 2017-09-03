package doobie.free

import cats.~>
import cats.effect.Async
import cats.free.{ Free => FF } // alias because some algebras have an op called Free

import java.lang.String
import java.sql.Ref
import java.util.Map

@SuppressWarnings(Array("org.wartremover.warts.Overloading"))
object ref { module =>

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
      def handleErrorWith[A](fa: RefIO[A], f: Throwable => RefIO[A]): F[A]
      def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A]

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
    final case class Delay[A](a: () => A) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    final case class HandleErrorWith[A](fa: RefIO[A], f: Throwable => RefIO[A]) extends RefOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa, f)
    }
    final case class Async1[A](k: (Either[Throwable, A] => Unit) => Unit) extends RefOp[A] {
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
  def raw[A](f: Ref => A): RefIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[RefOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def delay[A](a: => A): RefIO[A] = FF.liftF(Delay(() => a))
  def handleErrorWith[A](fa: RefIO[A], f: Throwable => RefIO[A]): RefIO[A] = FF.liftF[RefOp, A](HandleErrorWith(fa, f))
  def raiseError[A](err: Throwable): RefIO[A] = delay(throw err)
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): RefIO[A] = FF.liftF[RefOp, A](Async1(k))

  // Smart constructors for Ref-specific operations.
  val getBaseTypeName: RefIO[String] = FF.liftF(GetBaseTypeName)
  val getObject: RefIO[AnyRef] = FF.liftF(GetObject)
  def getObject(a: Map[String, Class[_]]): RefIO[AnyRef] = FF.liftF(GetObject1(a))
  def setObject(a: AnyRef): RefIO[Unit] = FF.liftF(SetObject(a))

  // RefIO is an Async
  implicit val AsyncRefIO: Async[RefIO] =
    new Async[RefIO] {
      val M = FF.catsFreeMonadForFree[RefOp]
      def pure[A](x: A): RefIO[A] = M.pure(x)
      def handleErrorWith[A](fa: RefIO[A])(f: Throwable => RefIO[A]): RefIO[A] = module.handleErrorWith(fa, f)
      def raiseError[A](e: Throwable): RefIO[A] = module.raiseError(e)
      def async[A](k: (Either[Throwable,A] => Unit) => Unit): RefIO[A] = module.async(k)
      def flatMap[A, B](fa: RefIO[A])(f: A => RefIO[B]): RefIO[B] = M.flatMap(fa)(f)
      def tailRecM[A, B](a: A)(f: A => RefIO[Either[A, B]]): RefIO[B] = M.tailRecM(a)(f)
      def suspend[A](thunk: => RefIO[A]): RefIO[A] = M.flatten(module.delay(thunk))
    }

}

