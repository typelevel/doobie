package doobie.free

import cats.~>
import cats.effect.Async
import cats.free.{ Free => FF } // alias because some algebras have an op called Free

import java.lang.String
import java.sql.SQLData
import java.sql.SQLInput
import java.sql.SQLOutput

object sqldata {

  // Algebra of operations for SQLData. Each accepts a visitor as an alternatie to pattern-matching.
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

    // Interface for a natural tansformation SQLDataOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (SQLDataOp ~> F) {
      final def apply[A](fa: SQLDataOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: SQLData => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def delay[A](a: () => A): F[A]
      def handleErrorWith[A](fa: SQLDataIO[A], f: Throwable => SQLDataIO[A]): F[A]
      def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A]

      // SQLData
      def getSQLTypeName: F[String]
      def readSQL(a: SQLInput, b: String): F[Unit]
      def writeSQL(a: SQLOutput): F[Unit]

    }

    // Common operations for all algebras.
    case class Raw[A](f: SQLData => A) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    case class Embed[A](e: Embedded[A]) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    case class Delay[A](a: () => A) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    case class HandleErrorWith[A](fa: SQLDataIO[A], f: Throwable => SQLDataIO[A]) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa, f)
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => Unit) extends SQLDataOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // SQLData-specific operations.
    case object GetSQLTypeName extends SQLDataOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.getSQLTypeName
    }
    case class  ReadSQL(a: SQLInput, b: String) extends SQLDataOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.readSQL(a, b)
    }
    case class  WriteSQL(a: SQLOutput) extends SQLDataOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.writeSQL(a)
    }

  }
  import SQLDataOp._

  // Smart constructors for operations common to all algebras.
  val unit: SQLDataIO[Unit] = FF.pure[SQLDataOp, Unit](())
  def raw[A](f: SQLData => A): SQLDataIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[SQLDataOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def delay[A](a: => A): SQLDataIO[A] = FF.liftF(Delay(() => a))
  def handleErrorWith[A](fa: SQLDataIO[A], f: Throwable => SQLDataIO[A]): SQLDataIO[A] = FF.liftF[SQLDataOp, A](HandleErrorWith(fa, f))
  def raiseError[A](err: Throwable): SQLDataIO[A] = delay(throw err)
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): SQLDataIO[A] = FF.liftF[SQLDataOp, A](Async1(k))

  // Smart constructors for SQLData-specific operations.
  val getSQLTypeName: SQLDataIO[String] = FF.liftF(GetSQLTypeName)
  def readSQL(a: SQLInput, b: String): SQLDataIO[Unit] = FF.liftF(ReadSQL(a, b))
  def writeSQL(a: SQLOutput): SQLDataIO[Unit] = FF.liftF(WriteSQL(a))

  // SQLDataIO is an Async
  implicit val AsyncSQLDataIO: Async[SQLDataIO] =
    new Async[SQLDataIO] {
      val M = FF.catsFreeMonadForFree[SQLDataOp]
      def pure[A](x: A): SQLDataIO[A] = M.pure(x)
      def handleErrorWith[A](fa: SQLDataIO[A])(f: Throwable => SQLDataIO[A]): SQLDataIO[A] = sqldata.handleErrorWith(fa, f)
      def raiseError[A](e: Throwable): SQLDataIO[A] = sqldata.raiseError(e)
      def async[A](k: (Either[Throwable,A] => Unit) => Unit): SQLDataIO[A] = sqldata.async(k)
      def flatMap[A, B](fa: SQLDataIO[A])(f: A => SQLDataIO[B]): SQLDataIO[B] = M.flatMap(fa)(f)
      def tailRecM[A, B](a: A)(f: A => SQLDataIO[Either[A, B]]): SQLDataIO[B] = M.tailRecM(a)(f)
      def suspend[A](thunk: => SQLDataIO[A]): SQLDataIO[A] = M.flatten(sqldata.delay(thunk))
    }

}

