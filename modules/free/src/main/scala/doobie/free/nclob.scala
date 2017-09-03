package doobie.free

import cats.~>
import cats.effect.Async
import cats.free.{ Free => FF } // alias because some algebras have an op called Free

import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.lang.String
import java.sql.Clob
import java.sql.NClob

@SuppressWarnings(Array("org.wartremover.warts.Overloading"))
object nclob { module =>

  // Algebra of operations for NClob. Each accepts a visitor as an alternatie to pattern-matching.
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

    // Interface for a natural tansformation NClobOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (NClobOp ~> F) {
      final def apply[A](fa: NClobOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: NClob => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def delay[A](a: () => A): F[A]
      def handleErrorWith[A](fa: NClobIO[A], f: Throwable => NClobIO[A]): F[A]
      def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A]

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
    final case class Delay[A](a: () => A) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    final case class HandleErrorWith[A](fa: NClobIO[A], f: Throwable => NClobIO[A]) extends NClobOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa, f)
    }
    final case class Async1[A](k: (Either[Throwable, A] => Unit) => Unit) extends NClobOp[A] {
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
  def raw[A](f: NClob => A): NClobIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[NClobOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def delay[A](a: => A): NClobIO[A] = FF.liftF(Delay(() => a))
  def handleErrorWith[A](fa: NClobIO[A], f: Throwable => NClobIO[A]): NClobIO[A] = FF.liftF[NClobOp, A](HandleErrorWith(fa, f))
  def raiseError[A](err: Throwable): NClobIO[A] = delay(throw err)
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): NClobIO[A] = FF.liftF[NClobOp, A](Async1(k))

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
      val M = FF.catsFreeMonadForFree[NClobOp]
      def pure[A](x: A): NClobIO[A] = M.pure(x)
      def handleErrorWith[A](fa: NClobIO[A])(f: Throwable => NClobIO[A]): NClobIO[A] = module.handleErrorWith(fa, f)
      def raiseError[A](e: Throwable): NClobIO[A] = module.raiseError(e)
      def async[A](k: (Either[Throwable,A] => Unit) => Unit): NClobIO[A] = module.async(k)
      def flatMap[A, B](fa: NClobIO[A])(f: A => NClobIO[B]): NClobIO[B] = M.flatMap(fa)(f)
      def tailRecM[A, B](a: A)(f: A => NClobIO[Either[A, B]]): NClobIO[B] = M.tailRecM(a)(f)
      def suspend[A](thunk: => NClobIO[A]): NClobIO[A] = M.flatten(module.delay(thunk))
    }

}

