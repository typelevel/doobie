// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.free

// Library imports
import cats.~>
import cats.data.Kleisli
import cats.effect.{ Async, ExitCase }

// Types referenced in the JDBC API
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.lang.Class
import java.lang.String
import java.sql.ResultSet
import java.sql.{ Array => SqlArray }
import org.postgresql.PGConnection
import org.postgresql.PGNotification
import org.postgresql.copy.{ CopyDual => PGCopyDual }
import org.postgresql.copy.{ CopyIn => PGCopyIn }
import org.postgresql.copy.{ CopyManager => PGCopyManager }
import org.postgresql.copy.{ CopyOut => PGCopyOut }
import org.postgresql.fastpath.FastpathArg
import org.postgresql.fastpath.{ Fastpath => PGFastpath }
import org.postgresql.jdbc.AutoSave
import org.postgresql.jdbc.PreferQueryMode
import org.postgresql.largeobject.LargeObject
import org.postgresql.largeobject.LargeObjectManager
import org.postgresql.replication.PGReplicationConnection

// Algebras and free monads thereof referenced by our interpreter.
import doobie.postgres.free.copyin.{ CopyInIO, CopyInOp }
import doobie.postgres.free.copymanager.{ CopyManagerIO, CopyManagerOp }
import doobie.postgres.free.copyout.{ CopyOutIO, CopyOutOp }
import doobie.postgres.free.fastpath.{ FastpathIO, FastpathOp }
import doobie.postgres.free.largeobject.{ LargeObjectIO, LargeObjectOp }
import doobie.postgres.free.largeobjectmanager.{ LargeObjectManagerIO, LargeObjectManagerOp }
import doobie.postgres.free.pgconnection.{ PGConnectionIO, PGConnectionOp }

object KleisliInterpreter {
  def apply[M[_]](implicit ev: Async[M]): KleisliInterpreter[M] =
    new KleisliInterpreter[M] {
      val M = ev
    }
}

// Family of interpreters into Kleisli arrows for some monad M.
trait KleisliInterpreter[M[_]] { outer =>
  implicit val M: Async[M]

  // The 7 interpreters, with definitions below. These can be overridden to customize behavior.
  lazy val CopyInInterpreter: CopyInOp ~> Kleisli[M, PGCopyIn, ?] = new CopyInInterpreter { }
  lazy val CopyManagerInterpreter: CopyManagerOp ~> Kleisli[M, PGCopyManager, ?] = new CopyManagerInterpreter { }
  lazy val CopyOutInterpreter: CopyOutOp ~> Kleisli[M, PGCopyOut, ?] = new CopyOutInterpreter { }
  lazy val FastpathInterpreter: FastpathOp ~> Kleisli[M, PGFastpath, ?] = new FastpathInterpreter { }
  lazy val LargeObjectInterpreter: LargeObjectOp ~> Kleisli[M, LargeObject, ?] = new LargeObjectInterpreter { }
  lazy val LargeObjectManagerInterpreter: LargeObjectManagerOp ~> Kleisli[M, LargeObjectManager, ?] = new LargeObjectManagerInterpreter { }
  lazy val PGConnectionInterpreter: PGConnectionOp ~> Kleisli[M, PGConnection, ?] = new PGConnectionInterpreter { }

  // Some methods are common to all interpreters and can be overridden to change behavior globally.
  def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(a => M.delay(f(a)))
  def delay[J, A](a: () => A): Kleisli[M, J, A] = Kleisli(_ => M.delay(a()))
  def raw[J, A](f: J => A): Kleisli[M, J, A] = primitive(f)
  def async[J, A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, J, A] = Kleisli(_ => M.async(k))
  def embed[J, A](e: Embedded[A]): Kleisli[M, J, A] =
    e match {
      case Embedded.CopyIn(j, fa) => Kleisli(_ => fa.foldMap(CopyInInterpreter).run(j))
      case Embedded.CopyManager(j, fa) => Kleisli(_ => fa.foldMap(CopyManagerInterpreter).run(j))
      case Embedded.CopyOut(j, fa) => Kleisli(_ => fa.foldMap(CopyOutInterpreter).run(j))
      case Embedded.Fastpath(j, fa) => Kleisli(_ => fa.foldMap(FastpathInterpreter).run(j))
      case Embedded.LargeObject(j, fa) => Kleisli(_ => fa.foldMap(LargeObjectInterpreter).run(j))
      case Embedded.LargeObjectManager(j, fa) => Kleisli(_ => fa.foldMap(LargeObjectManagerInterpreter).run(j))
      case Embedded.PGConnection(j, fa) => Kleisli(_ => fa.foldMap(PGConnectionInterpreter).run(j))
    }

  // Interpreters
  trait CopyInInterpreter extends CopyInOp.Visitor[Kleisli[M, PGCopyIn, ?]] {

    // common operations delegate to outer interpeter
    override def raw[A](f: PGCopyIn => A): Kleisli[M, PGCopyIn, A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, PGCopyIn, A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, PGCopyIn, A] = outer.delay(a)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, PGCopyIn, A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => CopyInIO[Unit]): Kleisli[M, PGCopyIn, A] =
      Kleisli(j => M.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: CopyInIO[A], f: Throwable => CopyInIO[A]): Kleisli[M, PGCopyIn, A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        M.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: CopyInIO[A])(use: A => CopyInIO[B])(release: (A, ExitCase[Throwable]) => CopyInIO[Unit]): Kleisli[M, PGCopyIn, B] =
      Kleisli(j => M.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    // domain-specific operations are implemented in terms of `primitive`
    override def cancelCopy = primitive(_.cancelCopy)
    override def endCopy = primitive(_.endCopy)
    override def flushCopy = primitive(_.flushCopy)
    override def getFieldCount = primitive(_.getFieldCount)
    override def getFieldFormat(a: Int) = primitive(_.getFieldFormat(a))
    override def getFormat = primitive(_.getFormat)
    override def getHandledRowCount = primitive(_.getHandledRowCount)
    override def isActive = primitive(_.isActive)
    override def writeToCopy(a: Array[Byte], b: Int, c: Int) = primitive(_.writeToCopy(a, b, c))

  }

  trait CopyManagerInterpreter extends CopyManagerOp.Visitor[Kleisli[M, PGCopyManager, ?]] {

    // common operations delegate to outer interpeter
    override def raw[A](f: PGCopyManager => A): Kleisli[M, PGCopyManager, A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, PGCopyManager, A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, PGCopyManager, A] = outer.delay(a)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, PGCopyManager, A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => CopyManagerIO[Unit]): Kleisli[M, PGCopyManager, A] =
      Kleisli(j => M.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: CopyManagerIO[A], f: Throwable => CopyManagerIO[A]): Kleisli[M, PGCopyManager, A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        M.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: CopyManagerIO[A])(use: A => CopyManagerIO[B])(release: (A, ExitCase[Throwable]) => CopyManagerIO[Unit]): Kleisli[M, PGCopyManager, B] =
      Kleisli(j => M.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    // domain-specific operations are implemented in terms of `primitive`
    override def copyDual(a: String) = primitive(_.copyDual(a))
    override def copyIn(a: String) = primitive(_.copyIn(a))
    override def copyIn(a: String, b: InputStream) = primitive(_.copyIn(a, b))
    override def copyIn(a: String, b: InputStream, c: Int) = primitive(_.copyIn(a, b, c))
    override def copyIn(a: String, b: Reader) = primitive(_.copyIn(a, b))
    override def copyIn(a: String, b: Reader, c: Int) = primitive(_.copyIn(a, b, c))
    override def copyOut(a: String) = primitive(_.copyOut(a))
    override def copyOut(a: String, b: OutputStream) = primitive(_.copyOut(a, b))
    override def copyOut(a: String, b: Writer) = primitive(_.copyOut(a, b))

  }

  trait CopyOutInterpreter extends CopyOutOp.Visitor[Kleisli[M, PGCopyOut, ?]] {

    // common operations delegate to outer interpeter
    override def raw[A](f: PGCopyOut => A): Kleisli[M, PGCopyOut, A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, PGCopyOut, A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, PGCopyOut, A] = outer.delay(a)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, PGCopyOut, A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => CopyOutIO[Unit]): Kleisli[M, PGCopyOut, A] =
      Kleisli(j => M.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: CopyOutIO[A], f: Throwable => CopyOutIO[A]): Kleisli[M, PGCopyOut, A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        M.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: CopyOutIO[A])(use: A => CopyOutIO[B])(release: (A, ExitCase[Throwable]) => CopyOutIO[Unit]): Kleisli[M, PGCopyOut, B] =
      Kleisli(j => M.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    // domain-specific operations are implemented in terms of `primitive`
    override def cancelCopy = primitive(_.cancelCopy)
    override def getFieldCount = primitive(_.getFieldCount)
    override def getFieldFormat(a: Int) = primitive(_.getFieldFormat(a))
    override def getFormat = primitive(_.getFormat)
    override def getHandledRowCount = primitive(_.getHandledRowCount)
    override def isActive = primitive(_.isActive)
    override def readFromCopy = primitive(_.readFromCopy)
    override def readFromCopy(a: Boolean) = primitive(_.readFromCopy(a))

  }

  trait FastpathInterpreter extends FastpathOp.Visitor[Kleisli[M, PGFastpath, ?]] {

    // common operations delegate to outer interpeter
    override def raw[A](f: PGFastpath => A): Kleisli[M, PGFastpath, A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, PGFastpath, A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, PGFastpath, A] = outer.delay(a)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, PGFastpath, A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => FastpathIO[Unit]): Kleisli[M, PGFastpath, A] =
      Kleisli(j => M.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: FastpathIO[A], f: Throwable => FastpathIO[A]): Kleisli[M, PGFastpath, A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        M.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: FastpathIO[A])(use: A => FastpathIO[B])(release: (A, ExitCase[Throwable]) => FastpathIO[Unit]): Kleisli[M, PGFastpath, B] =
      Kleisli(j => M.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    // domain-specific operations are implemented in terms of `primitive`
    override def addFunction(a: String, b: Int) = primitive(_.addFunction(a, b))
    override def addFunctions(a: ResultSet) = primitive(_.addFunctions(a))
    override def fastpath(a: Int, b: Array[FastpathArg]) = primitive(_.fastpath(a, b))
    override def fastpath(a: Int, b: Boolean, c: Array[FastpathArg]) = primitive(_.fastpath(a, b, c))
    override def fastpath(a: String, b: Array[FastpathArg]) = primitive(_.fastpath(a, b))
    override def fastpath(a: String, b: Boolean, c: Array[FastpathArg]) = primitive(_.fastpath(a, b, c))
    override def getData(a: String, b: Array[FastpathArg]) = primitive(_.getData(a, b))
    override def getID(a: String) = primitive(_.getID(a))
    override def getInteger(a: String, b: Array[FastpathArg]) = primitive(_.getInteger(a, b))
    override def getLong(a: String, b: Array[FastpathArg]) = primitive(_.getLong(a, b))
    override def getOID(a: String, b: Array[FastpathArg]) = primitive(_.getOID(a, b))

  }

  trait LargeObjectInterpreter extends LargeObjectOp.Visitor[Kleisli[M, LargeObject, ?]] {

    // common operations delegate to outer interpeter
    override def raw[A](f: LargeObject => A): Kleisli[M, LargeObject, A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, LargeObject, A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, LargeObject, A] = outer.delay(a)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, LargeObject, A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => LargeObjectIO[Unit]): Kleisli[M, LargeObject, A] =
      Kleisli(j => M.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: LargeObjectIO[A], f: Throwable => LargeObjectIO[A]): Kleisli[M, LargeObject, A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        M.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: LargeObjectIO[A])(use: A => LargeObjectIO[B])(release: (A, ExitCase[Throwable]) => LargeObjectIO[Unit]): Kleisli[M, LargeObject, B] =
      Kleisli(j => M.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    // domain-specific operations are implemented in terms of `primitive`
    override def close = primitive(_.close)
    override def copy = primitive(_.copy)
    override def getInputStream = primitive(_.getInputStream)
    override def getInputStream(a: Long) = primitive(_.getInputStream(a))
    override def getLongOID = primitive(_.getLongOID)
    override def getOID = primitive(_.getOID)
    override def getOutputStream = primitive(_.getOutputStream)
    override def read(a: Array[Byte], b: Int, c: Int) = primitive(_.read(a, b, c))
    override def read(a: Int) = primitive(_.read(a))
    override def seek(a: Int) = primitive(_.seek(a))
    override def seek(a: Int, b: Int) = primitive(_.seek(a, b))
    override def seek64(a: Long, b: Int) = primitive(_.seek64(a, b))
    override def size = primitive(_.size)
    override def size64 = primitive(_.size64)
    override def tell = primitive(_.tell)
    override def tell64 = primitive(_.tell64)
    override def truncate(a: Int) = primitive(_.truncate(a))
    override def truncate64(a: Long) = primitive(_.truncate64(a))
    override def write(a: Array[Byte]) = primitive(_.write(a))
    override def write(a: Array[Byte], b: Int, c: Int) = primitive(_.write(a, b, c))

  }

  trait LargeObjectManagerInterpreter extends LargeObjectManagerOp.Visitor[Kleisli[M, LargeObjectManager, ?]] {

    // common operations delegate to outer interpeter
    override def raw[A](f: LargeObjectManager => A): Kleisli[M, LargeObjectManager, A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, LargeObjectManager, A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, LargeObjectManager, A] = outer.delay(a)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, LargeObjectManager, A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => LargeObjectManagerIO[Unit]): Kleisli[M, LargeObjectManager, A] =
      Kleisli(j => M.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: LargeObjectManagerIO[A], f: Throwable => LargeObjectManagerIO[A]): Kleisli[M, LargeObjectManager, A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        M.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: LargeObjectManagerIO[A])(use: A => LargeObjectManagerIO[B])(release: (A, ExitCase[Throwable]) => LargeObjectManagerIO[Unit]): Kleisli[M, LargeObjectManager, B] =
      Kleisli(j => M.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    // domain-specific operations are implemented in terms of `primitive`
    override def create = primitive(_.create)
    override def create(a: Int) = primitive(_.create(a))
    override def createLO = primitive(_.createLO)
    override def createLO(a: Int) = primitive(_.createLO(a))
    override def delete(a: Int) = primitive(_.delete(a))
    override def delete(a: Long) = primitive(_.delete(a))
    override def open(a: Int) = primitive(_.open(a))
    override def open(a: Int, b: Boolean) = primitive(_.open(a, b))
    override def open(a: Int, b: Int) = primitive(_.open(a, b))
    override def open(a: Int, b: Int, c: Boolean) = primitive(_.open(a, b, c))
    override def open(a: Long) = primitive(_.open(a))
    override def open(a: Long, b: Boolean) = primitive(_.open(a, b))
    override def open(a: Long, b: Int) = primitive(_.open(a, b))
    override def open(a: Long, b: Int, c: Boolean) = primitive(_.open(a, b, c))
    override def unlink(a: Int) = primitive(_.unlink(a))
    override def unlink(a: Long) = primitive(_.unlink(a))

  }

  trait PGConnectionInterpreter extends PGConnectionOp.Visitor[Kleisli[M, PGConnection, ?]] {

    // common operations delegate to outer interpeter
    override def raw[A](f: PGConnection => A): Kleisli[M, PGConnection, A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, PGConnection, A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, PGConnection, A] = outer.delay(a)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, PGConnection, A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => PGConnectionIO[Unit]): Kleisli[M, PGConnection, A] =
      Kleisli(j => M.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: PGConnectionIO[A], f: Throwable => PGConnectionIO[A]): Kleisli[M, PGConnection, A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        M.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: PGConnectionIO[A])(use: A => PGConnectionIO[B])(release: (A, ExitCase[Throwable]) => PGConnectionIO[Unit]): Kleisli[M, PGConnection, B] =
      Kleisli(j => M.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    // domain-specific operations are implemented in terms of `primitive`
    override def addDataType(a: String, b: Class[_ <: org.postgresql.util.PGobject]) = primitive(_.addDataType(a, b))
    override def addDataType(a: String, b: String) = primitive(_.addDataType(a, b))
    override def createArrayOf(a: String, b: AnyRef) = primitive(_.createArrayOf(a, b))
    override def escapeIdentifier(a: String) = primitive(_.escapeIdentifier(a))
    override def escapeLiteral(a: String) = primitive(_.escapeLiteral(a))
    override def getAutosave = primitive(_.getAutosave)
    override def getBackendPID = primitive(_.getBackendPID)
    override def getCopyAPI = primitive(_.getCopyAPI)
    override def getDefaultFetchSize = primitive(_.getDefaultFetchSize)
    override def getFastpathAPI = primitive(_.getFastpathAPI)
    override def getLargeObjectAPI = primitive(_.getLargeObjectAPI)
    override def getNotifications = primitive(_.getNotifications)
    override def getNotifications(a: Int) = primitive(_.getNotifications(a))
    override def getPreferQueryMode = primitive(_.getPreferQueryMode)
    override def getPrepareThreshold = primitive(_.getPrepareThreshold)
    override def getReplicationAPI = primitive(_.getReplicationAPI)
    override def setAutosave(a: AutoSave) = primitive(_.setAutosave(a))
    override def setDefaultFetchSize(a: Int) = primitive(_.setDefaultFetchSize(a))
    override def setPrepareThreshold(a: Int) = primitive(_.setPrepareThreshold(a))

  }


}

