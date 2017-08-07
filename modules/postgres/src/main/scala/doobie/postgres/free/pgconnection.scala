package doobie.postgres.free

import cats.~>
import cats.effect.Async
import cats.free.{ Free => FF } // alias because some algebras have an op called Free

import java.lang.Class
import java.lang.String
import org.postgresql.PGConnection
import org.postgresql.PGNotification
import org.postgresql.copy.{ CopyManager => PGCopyManager }
import org.postgresql.fastpath.{ Fastpath => PGFastpath }
import org.postgresql.jdbc.AutoSave
import org.postgresql.jdbc.PreferQueryMode
import org.postgresql.largeobject.LargeObjectManager
import org.postgresql.replication.PGReplicationConnection

object pgconnection { module =>

  // Algebra of operations for PGConnection. Each accepts a visitor as an alternatie to pattern-matching.
  sealed trait PGConnectionOp[A] {
    def visit[F[_]](v: PGConnectionOp.Visitor[F]): F[A]
  }

  // Free monad over PGConnectionOp.
  type PGConnectionIO[A] = FF[PGConnectionOp, A]

  // Module of instances and constructors of PGConnectionOp.
  object PGConnectionOp {

    // Given a PGConnection we can embed a PGConnectionIO program in any algebra that understands embedding.
    implicit val PGConnectionOpEmbeddable: Embeddable[PGConnectionOp, PGConnection] =
      new Embeddable[PGConnectionOp, PGConnection] {
        def embed[A](j: PGConnection, fa: FF[PGConnectionOp, A]) = Embedded.PGConnection(j, fa)
      }

    // Interface for a natural tansformation PGConnectionOp ~> F encoded via the visitor pattern.
    // This approach is much more efficient than pattern-matching for large algebras.
    trait Visitor[F[_]] extends (PGConnectionOp ~> F) {
      final def apply[A](fa: PGConnectionOp[A]): F[A] = fa.visit(this)

      // Common
      def raw[A](f: PGConnection => A): F[A]
      def embed[A](e: Embedded[A]): F[A]
      def delay[A](a: () => A): F[A]
      def handleErrorWith[A](fa: PGConnectionIO[A], f: Throwable => PGConnectionIO[A]): F[A]
      def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A]

      // PGConnection
      def addDataType(a: String, b: Class[_ <: org.postgresql.util.PGobject]): F[Unit]
      def addDataType(a: String, b: String): F[Unit]
      def escapeIdentifier(a: String): F[String]
      def escapeLiteral(a: String): F[String]
      def getAutosave: F[AutoSave]
      def getBackendPID: F[Int]
      def getCopyAPI: F[PGCopyManager]
      def getDefaultFetchSize: F[Int]
      def getFastpathAPI: F[PGFastpath]
      def getLargeObjectAPI: F[LargeObjectManager]
      def getNotifications: F[Array[PGNotification]]
      def getNotifications(a: Int): F[Array[PGNotification]]
      def getPreferQueryMode: F[PreferQueryMode]
      def getPrepareThreshold: F[Int]
      def getReplicationAPI: F[PGReplicationConnection]
      def setAutosave(a: AutoSave): F[Unit]
      def setDefaultFetchSize(a: Int): F[Unit]
      def setPrepareThreshold(a: Int): F[Unit]

    }

    // Common operations for all algebras.
    case class Raw[A](f: PGConnection => A) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    }
    case class Embed[A](e: Embedded[A]) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    }
    case class Delay[A](a: () => A) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    }
    case class HandleErrorWith[A](fa: PGConnectionIO[A], f: Throwable => PGConnectionIO[A]) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.handleErrorWith(fa, f)
    }
    case class Async1[A](k: (Either[Throwable, A] => Unit) => Unit) extends PGConnectionOp[A] {
      def visit[F[_]](v: Visitor[F]) = v.async(k)
    }

    // PGConnection-specific operations.
    case class  AddDataType(a: String, b: Class[_ <: org.postgresql.util.PGobject]) extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.addDataType(a, b)
    }
    case class  AddDataType1(a: String, b: String) extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.addDataType(a, b)
    }
    case class  EscapeIdentifier(a: String) extends PGConnectionOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.escapeIdentifier(a)
    }
    case class  EscapeLiteral(a: String) extends PGConnectionOp[String] {
      def visit[F[_]](v: Visitor[F]) = v.escapeLiteral(a)
    }
    case object GetAutosave extends PGConnectionOp[AutoSave] {
      def visit[F[_]](v: Visitor[F]) = v.getAutosave
    }
    case object GetBackendPID extends PGConnectionOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getBackendPID
    }
    case object GetCopyAPI extends PGConnectionOp[PGCopyManager] {
      def visit[F[_]](v: Visitor[F]) = v.getCopyAPI
    }
    case object GetDefaultFetchSize extends PGConnectionOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getDefaultFetchSize
    }
    case object GetFastpathAPI extends PGConnectionOp[PGFastpath] {
      def visit[F[_]](v: Visitor[F]) = v.getFastpathAPI
    }
    case object GetLargeObjectAPI extends PGConnectionOp[LargeObjectManager] {
      def visit[F[_]](v: Visitor[F]) = v.getLargeObjectAPI
    }
    case object GetNotifications extends PGConnectionOp[Array[PGNotification]] {
      def visit[F[_]](v: Visitor[F]) = v.getNotifications
    }
    case class  GetNotifications1(a: Int) extends PGConnectionOp[Array[PGNotification]] {
      def visit[F[_]](v: Visitor[F]) = v.getNotifications(a)
    }
    case object GetPreferQueryMode extends PGConnectionOp[PreferQueryMode] {
      def visit[F[_]](v: Visitor[F]) = v.getPreferQueryMode
    }
    case object GetPrepareThreshold extends PGConnectionOp[Int] {
      def visit[F[_]](v: Visitor[F]) = v.getPrepareThreshold
    }
    case object GetReplicationAPI extends PGConnectionOp[PGReplicationConnection] {
      def visit[F[_]](v: Visitor[F]) = v.getReplicationAPI
    }
    case class  SetAutosave(a: AutoSave) extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setAutosave(a)
    }
    case class  SetDefaultFetchSize(a: Int) extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setDefaultFetchSize(a)
    }
    case class  SetPrepareThreshold(a: Int) extends PGConnectionOp[Unit] {
      def visit[F[_]](v: Visitor[F]) = v.setPrepareThreshold(a)
    }

  }
  import PGConnectionOp._

  // Smart constructors for operations common to all algebras.
  val unit: PGConnectionIO[Unit] = FF.pure[PGConnectionOp, Unit](())
  def raw[A](f: PGConnection => A): PGConnectionIO[A] = FF.liftF(Raw(f))
  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[PGConnectionOp, A] = FF.liftF(Embed(ev.embed(j, fa)))
  def delay[A](a: => A): PGConnectionIO[A] = FF.liftF(Delay(() => a))
  def handleErrorWith[A](fa: PGConnectionIO[A], f: Throwable => PGConnectionIO[A]): PGConnectionIO[A] = FF.liftF[PGConnectionOp, A](HandleErrorWith(fa, f))
  def raiseError[A](err: Throwable): PGConnectionIO[A] = delay(throw err)
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): PGConnectionIO[A] = FF.liftF[PGConnectionOp, A](Async1(k))

  // Smart constructors for PGConnection-specific operations.
  def addDataType(a: String, b: Class[_ <: org.postgresql.util.PGobject]): PGConnectionIO[Unit] = FF.liftF(AddDataType(a, b))
  def addDataType(a: String, b: String): PGConnectionIO[Unit] = FF.liftF(AddDataType1(a, b))
  def escapeIdentifier(a: String): PGConnectionIO[String] = FF.liftF(EscapeIdentifier(a))
  def escapeLiteral(a: String): PGConnectionIO[String] = FF.liftF(EscapeLiteral(a))
  val getAutosave: PGConnectionIO[AutoSave] = FF.liftF(GetAutosave)
  val getBackendPID: PGConnectionIO[Int] = FF.liftF(GetBackendPID)
  val getCopyAPI: PGConnectionIO[PGCopyManager] = FF.liftF(GetCopyAPI)
  val getDefaultFetchSize: PGConnectionIO[Int] = FF.liftF(GetDefaultFetchSize)
  val getFastpathAPI: PGConnectionIO[PGFastpath] = FF.liftF(GetFastpathAPI)
  val getLargeObjectAPI: PGConnectionIO[LargeObjectManager] = FF.liftF(GetLargeObjectAPI)
  val getNotifications: PGConnectionIO[Array[PGNotification]] = FF.liftF(GetNotifications)
  def getNotifications(a: Int): PGConnectionIO[Array[PGNotification]] = FF.liftF(GetNotifications1(a))
  val getPreferQueryMode: PGConnectionIO[PreferQueryMode] = FF.liftF(GetPreferQueryMode)
  val getPrepareThreshold: PGConnectionIO[Int] = FF.liftF(GetPrepareThreshold)
  val getReplicationAPI: PGConnectionIO[PGReplicationConnection] = FF.liftF(GetReplicationAPI)
  def setAutosave(a: AutoSave): PGConnectionIO[Unit] = FF.liftF(SetAutosave(a))
  def setDefaultFetchSize(a: Int): PGConnectionIO[Unit] = FF.liftF(SetDefaultFetchSize(a))
  def setPrepareThreshold(a: Int): PGConnectionIO[Unit] = FF.liftF(SetPrepareThreshold(a))

  // PGConnectionIO is an Async
  implicit val AsyncPGConnectionIO: Async[PGConnectionIO] =
    new Async[PGConnectionIO] {
      val M = FF.catsFreeMonadForFree[PGConnectionOp]
      def pure[A](x: A): PGConnectionIO[A] = M.pure(x)
      def handleErrorWith[A](fa: PGConnectionIO[A])(f: Throwable => PGConnectionIO[A]): PGConnectionIO[A] = module.handleErrorWith(fa, f)
      def raiseError[A](e: Throwable): PGConnectionIO[A] = module.raiseError(e)
      def async[A](k: (Either[Throwable,A] => Unit) => Unit): PGConnectionIO[A] = module.async(k)
      def flatMap[A, B](fa: PGConnectionIO[A])(f: A => PGConnectionIO[B]): PGConnectionIO[B] = M.flatMap(fa)(f)
      def tailRecM[A, B](a: A)(f: A => PGConnectionIO[Either[A, B]]): PGConnectionIO[B] = M.tailRecM(a)(f)
      def suspend[A](thunk: => PGConnectionIO[A]): PGConnectionIO[A] = M.flatten(module.delay(thunk))
    }

}

