package doobie.postgres.free

import cats.~>
import cats.effect.Async
import cats.free.{ Free => FF } // alias because some algebras have an op called Free

import java.lang.String
import java.sql.ResultSet
import org.postgresql.fastpath.FastpathArg
import org.postgresql.fastpath.{ Fastpath => PGFastpath }

object fastpath { module =>

  // Algebra of operations for PGFastpath. Each accepts a visitor as an alternatie to pattern-matching.
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

    // Interface for a natural tansformation FastpathOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (FastpathOp ~> F) {
      final def apply[A](fa: FastpathOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: PGFastpath => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def delay[A](a: () => A): F[A]
      def handleErrorWith[A](fa: FastpathIO[A], f: Throwable => FastpathIO[A]): F[A]
      def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A]

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
    case class Raw[A](f: PGFastpath => A) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    case class Embed[A](e: Embedded[A]) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    case class Delay[A](a: () => A) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    case class HandleErrorWith[A](fa: FastpathIO[A], f: Throwable => FastpathIO[A]) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa, f)
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => Unit) extends FastpathOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // PGFastpath-specific operations.
    case class  AddFunction(a: String, b: Int) extends FastpathOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.addFunction(a, b)
    }
    case class  AddFunctions(a: ResultSet) extends FastpathOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.addFunctions(a)
    }
    case class  Fastpath(a: Int, b: Array[FastpathArg]) extends FastpathOp[Array[Byte]] {
      def visit[F[_]](v: Visitor[F]) = v.fastpath(a, b)
    }
    case class  Fastpath1(a: Int, b: Boolean, c: Array[FastpathArg]) extends FastpathOp[AnyRef] {
      def visit[F[_]](v: Visitor[F]) = v.fastpath(a, b, c)
    }
    case class  Fastpath2(a: String, b: Array[FastpathArg]) extends FastpathOp[Array[Byte]] {
      def visit[F[_]](v: Visitor[F]) = v.fastpath(a, b)
    }
    case class  Fastpath3(a: String, b: Boolean, c: Array[FastpathArg]) extends FastpathOp[AnyRef] {
      def visit[F[_]](v: Visitor[F]) = v.fastpath(a, b, c)
    }
    case class  GetData(a: String, b: Array[FastpathArg]) extends FastpathOp[Array[Byte]] {
      def visit[F[_]](v: Visitor[F]) = v.getData(a, b)
    }
    case class  GetID(a: String) extends FastpathOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getID(a)
    }
    case class  GetInteger(a: String, b: Array[FastpathArg]) extends FastpathOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getInteger(a, b)
    }
    case class  GetLong(a: String, b: Array[FastpathArg]) extends FastpathOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.getLong(a, b)
    }
    case class  GetOID(a: String, b: Array[FastpathArg]) extends FastpathOp[Long] {
      def visit[F[_]](v: Visitor[F]) = v.getOID(a, b)
    }

  }
  import FastpathOp._

  // Smart constructors for operations common to all algebras.
  val unit: FastpathIO[Unit] = FF.pure[FastpathOp, Unit](())
  def raw[A](f: PGFastpath => A): FastpathIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[FastpathOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def delay[A](a: => A): FastpathIO[A] = FF.liftF(Delay(() => a))
  def handleErrorWith[A](fa: FastpathIO[A], f: Throwable => FastpathIO[A]): FastpathIO[A] = FF.liftF[FastpathOp, A](HandleErrorWith(fa, f))
  def raiseError[A](err: Throwable): FastpathIO[A] = delay(throw err)
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): FastpathIO[A] = FF.liftF[FastpathOp, A](Async1(k))

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
      val M = FF.catsFreeMonadForFree[FastpathOp]
      def pure[A](x: A): FastpathIO[A] = M.pure(x)
      def handleErrorWith[A](fa: FastpathIO[A])(f: Throwable => FastpathIO[A]): FastpathIO[A] = module.handleErrorWith(fa, f)
      def raiseError[A](e: Throwable): FastpathIO[A] = module.raiseError(e)
      def async[A](k: (Either[Throwable,A] => Unit) => Unit): FastpathIO[A] = module.async(k)
      def flatMap[A, B](fa: FastpathIO[A])(f: A => FastpathIO[B]): FastpathIO[B] = M.flatMap(fa)(f)
      def tailRecM[A, B](a: A)(f: A => FastpathIO[Either[A, B]]): FastpathIO[B] = M.tailRecM(a)(f)
      def suspend[A](thunk: => FastpathIO[A]): FastpathIO[A] = M.flatten(module.delay(thunk))
    }

}

