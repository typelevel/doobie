// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.free

// Library imports
import cats.~>
import cats.data.Kleisli
import cats.effect.{ Async, Sync }
import cats.free.Free
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import com.github.ghik.silencer.silent

// Types referenced in the JDBC API
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.lang.Class
import java.lang.String
import java.sql.ResultSet
import org.postgresql.PGConnection
import org.postgresql.copy.{ CopyIn => PGCopyIn }
import org.postgresql.copy.{ CopyManager => PGCopyManager }
import org.postgresql.copy.{ CopyOut => PGCopyOut }
import org.postgresql.fastpath.FastpathArg
import org.postgresql.fastpath.{ Fastpath => PGFastpath }
import org.postgresql.jdbc.AutoSave
import org.postgresql.largeobject.LargeObject
import org.postgresql.largeobject.LargeObjectManager

// Algebras and free monads thereof referenced by our interpreter.
import doobie.postgres.free.copyin.{ CopyInIO, CopyInOp }
import doobie.postgres.free.copymanager.{ CopyManagerIO, CopyManagerOp }
import doobie.postgres.free.copyout.{ CopyOutIO, CopyOutOp }
import doobie.postgres.free.fastpath.{ FastpathIO, FastpathOp }
import doobie.postgres.free.largeobject.{ LargeObjectIO, LargeObjectOp }
import doobie.postgres.free.largeobjectmanager.{ LargeObjectManagerIO, LargeObjectManagerOp }
import doobie.postgres.free.pgconnection.{ PGConnectionIO, PGConnectionOp }

object KleisliInterpreter {

  def apply[M[_]](
    implicit am: Async[M]
  ): KleisliInterpreter[M] =
    new KleisliInterpreter[M] {
      val asyncM = am
    }

}

// Family of interpreters into Kleisli arrows for some monad M.
@silent("deprecated")
trait KleisliInterpreter[M[_]] { outer =>

  implicit val asyncM: Async[M]

  // The 7 interpreters, with definitions below. These can be overridden to customize behavior.
  lazy val CopyInInterpreter: CopyInOp ~> Kleisli[M, PGCopyIn, *] = new CopyInInterpreter { }
  lazy val CopyManagerInterpreter: CopyManagerOp ~> Kleisli[M, PGCopyManager, *] = new CopyManagerInterpreter { }
  lazy val CopyOutInterpreter: CopyOutOp ~> Kleisli[M, PGCopyOut, *] = new CopyOutInterpreter { }
  lazy val FastpathInterpreter: FastpathOp ~> Kleisli[M, PGFastpath, *] = new FastpathInterpreter { }
  lazy val LargeObjectInterpreter: LargeObjectOp ~> Kleisli[M, LargeObject, *] = new LargeObjectInterpreter { }
  lazy val LargeObjectManagerInterpreter: LargeObjectManagerOp ~> Kleisli[M, LargeObjectManager, *] = new LargeObjectManagerInterpreter { }
  lazy val PGConnectionInterpreter: PGConnectionOp ~> Kleisli[M, PGConnection, *] = new PGConnectionInterpreter { }

  // Some methods are common to all interpreters and can be overridden to change behavior globally.
  def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli { a =>
    // primitive JDBC methods throw exceptions and so do we when reading values
    // so catch any non-fatal exceptions and lift them into the effect
    try {
      asyncM.blocking(f(a))
    } catch {
      case scala.util.control.NonFatal(e) => asyncM.raiseError(e)
    }
  }
  def delay[J, A](thunk: => A): Kleisli[M, J, A] = Kleisli(_ => asyncM.delay(thunk))
  def raw[J, A](f: J => A): Kleisli[M, J, A] = primitive(f)
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
  def raiseError[J, A](e: Throwable): Kleisli[M, J, A] = Kleisli(_ => asyncM.raiseError(e))
  def monotonic[J]: Kleisli[M, J, FiniteDuration] = Kleisli(_ => asyncM.monotonic)
  def realTime[J]: Kleisli[M, J, FiniteDuration] = Kleisli(_ => asyncM.realTime)
  def suspend[J, A](hint: Sync.Type)(thunk: => A): Kleisli[M, J, A] = Kleisli(_ => asyncM.suspend(hint)(thunk))
  def canceled[J]: Kleisli[M, J, Unit] = Kleisli(_ => asyncM.canceled)
  def cede[J]: Kleisli[M, J, Unit] = Kleisli(_ => asyncM.cede)
  def sleep[J](time: FiniteDuration): Kleisli[M, J, Unit] = Kleisli(_ => asyncM.sleep(time))
  def executionContext[J]: Kleisli[M, J, ExecutionContext] = Kleisli(_ => asyncM.executionContext)
  // for operations using free structures we call the interpreter recursively
  def handleErrorWith[G[_], J, A](interpreter: G ~> Kleisli[M, J, *])(fa: Free[G, A])(f: Throwable => Free[G, A]): Kleisli[M, J, A] = Kleisli (j =>
    asyncM.handleErrorWith(fa.foldMap(interpreter).run(j))(f.andThen(_.foldMap(interpreter).run(j)))
  )
  def forceR[G[_], J, A, B](interpreter: G ~> Kleisli[M, J, *])(fa: Free[G, A])(fb: Free[G, B]): Kleisli[M, J, B] = Kleisli (j =>
      asyncM.forceR(fa.foldMap(interpreter).run(j))(fb.foldMap(interpreter).run(j))
    )
  def onCancel[G[_], J, A](interpreter: G ~> Kleisli[M, J, *])(fa: Free[G, A], fin: Free[G, Unit]): Kleisli[M, J, A] = Kleisli (j =>
    asyncM.onCancel(fa.foldMap(interpreter).run(j), fin.foldMap(interpreter).run(j))
  )
  def evalOn[G[_], J, A](interpreter: G ~> Kleisli[M, J, *])(fa: Free[G, A], ec: ExecutionContext): Kleisli[M, J, A] = Kleisli(j => 
    asyncM.evalOn(fa.foldMap(interpreter).run(j), ec)
  )
  def async[G[_], J, A](interpreter: G ~> Kleisli[M, J, *])(k: (Either[Throwable, A] => Unit) => Free[G, Option[Free[G, Unit]]]): Kleisli[M, J, A] = Kleisli(j =>
    asyncM.async(k.andThen(c => asyncM.map(c.foldMap(interpreter).run(j))(_.map(_.foldMap(interpreter).run(j)))))
  )

  // Interpreters
  trait CopyInInterpreter extends CopyInOp.Visitor[Kleisli[M, PGCopyIn, *]] {

    // common operations delegate to outer interpreter
    override def delay[A](thunk: => A) = outer.delay(thunk)
    override def raw[A](f: PGCopyIn => A) = outer.raw(f)
    override def embed[A](e: Embedded[A]) = outer.embed(e)
    override def raiseError[A](e: Throwable): Kleisli[M, PGCopyIn, A] = outer.raiseError(e)
    override def monotonic: Kleisli[M, PGCopyIn, FiniteDuration] = outer.monotonic
    override def realTime: Kleisli[M, PGCopyIn, FiniteDuration] = outer.realTime
    override def suspend[A](hint: Sync.Type)(thunk: => A): Kleisli[M, PGCopyIn, A] = outer.suspend(hint)(thunk)
    override def canceled: Kleisli[M, PGCopyIn, Unit] = outer.canceled
    override def cede: Kleisli[M, PGCopyIn, Unit] = outer.cede
    override def sleep(time: FiniteDuration): Kleisli[M, PGCopyIn, Unit] = outer.sleep(time)
    override def executionContext: Kleisli[M, PGCopyIn, ExecutionContext] = outer.executionContext
    
    // for operations using CopyInIO we must call ourself recursively
    override def handleErrorWith[A](fa: CopyInIO[A])(f: Throwable => CopyInIO[A]): Kleisli[M, PGCopyIn, A] = outer.handleErrorWith(this)(fa)(f)
    override def forceR[A, B](fa: CopyInIO[A])(fb: CopyInIO[B]): Kleisli[M, PGCopyIn, B] = outer.forceR(this)(fa)(fb)
    override def onCancel[A](fa: CopyInIO[A], fin: CopyInIO[Unit]): Kleisli[M, PGCopyIn, A] = outer.onCancel(this)(fa, fin)
    override def evalOn[A](fa: CopyInIO[A], ec: ExecutionContext): Kleisli[M, PGCopyIn, A] = outer.evalOn(this)(fa, ec)
    override def async[A](k: (Either[Throwable, A] => Unit) => CopyInIO[Option[CopyInIO[Unit]]]) = outer.async(this)(k)

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

  trait CopyManagerInterpreter extends CopyManagerOp.Visitor[Kleisli[M, PGCopyManager, *]] {

    // common operations delegate to outer interpreter
    override def delay[A](thunk: => A) = outer.delay(thunk)
    override def raw[A](f: PGCopyManager => A) = outer.raw(f)
    override def embed[A](e: Embedded[A]) = outer.embed(e)
    override def raiseError[A](e: Throwable): Kleisli[M, PGCopyManager, A] = outer.raiseError(e)
    override def monotonic: Kleisli[M, PGCopyManager, FiniteDuration] = outer.monotonic
    override def realTime: Kleisli[M, PGCopyManager, FiniteDuration] = outer.realTime
    override def suspend[A](hint: Sync.Type)(thunk: => A): Kleisli[M, PGCopyManager, A] = outer.suspend(hint)(thunk)
    override def canceled: Kleisli[M, PGCopyManager, Unit] = outer.canceled
    override def cede: Kleisli[M, PGCopyManager, Unit] = outer.cede
    override def sleep(time: FiniteDuration): Kleisli[M, PGCopyManager, Unit] = outer.sleep(time)
    override def executionContext: Kleisli[M, PGCopyManager, ExecutionContext] = outer.executionContext
    
    // for operations using CopyManagerIO we must call ourself recursively
    override def handleErrorWith[A](fa: CopyManagerIO[A])(f: Throwable => CopyManagerIO[A]): Kleisli[M, PGCopyManager, A] = outer.handleErrorWith(this)(fa)(f)
    override def forceR[A, B](fa: CopyManagerIO[A])(fb: CopyManagerIO[B]): Kleisli[M, PGCopyManager, B] = outer.forceR(this)(fa)(fb)
    override def onCancel[A](fa: CopyManagerIO[A], fin: CopyManagerIO[Unit]): Kleisli[M, PGCopyManager, A] = outer.onCancel(this)(fa, fin)
    override def evalOn[A](fa: CopyManagerIO[A], ec: ExecutionContext): Kleisli[M, PGCopyManager, A] = outer.evalOn(this)(fa, ec)
    override def async[A](k: (Either[Throwable, A] => Unit) => CopyManagerIO[Option[CopyManagerIO[Unit]]]) = outer.async(this)(k)

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

  trait CopyOutInterpreter extends CopyOutOp.Visitor[Kleisli[M, PGCopyOut, *]] {

    // common operations delegate to outer interpreter
    override def delay[A](thunk: => A) = outer.delay(thunk)
    override def raw[A](f: PGCopyOut => A) = outer.raw(f)
    override def embed[A](e: Embedded[A]) = outer.embed(e)
    override def raiseError[A](e: Throwable): Kleisli[M, PGCopyOut, A] = outer.raiseError(e)
    override def monotonic: Kleisli[M, PGCopyOut, FiniteDuration] = outer.monotonic
    override def realTime: Kleisli[M, PGCopyOut, FiniteDuration] = outer.realTime
    override def suspend[A](hint: Sync.Type)(thunk: => A): Kleisli[M, PGCopyOut, A] = outer.suspend(hint)(thunk)
    override def canceled: Kleisli[M, PGCopyOut, Unit] = outer.canceled
    override def cede: Kleisli[M, PGCopyOut, Unit] = outer.cede
    override def sleep(time: FiniteDuration): Kleisli[M, PGCopyOut, Unit] = outer.sleep(time)
    override def executionContext: Kleisli[M, PGCopyOut, ExecutionContext] = outer.executionContext
    
    // for operations using CopyOutIO we must call ourself recursively
    override def handleErrorWith[A](fa: CopyOutIO[A])(f: Throwable => CopyOutIO[A]): Kleisli[M, PGCopyOut, A] = outer.handleErrorWith(this)(fa)(f)
    override def forceR[A, B](fa: CopyOutIO[A])(fb: CopyOutIO[B]): Kleisli[M, PGCopyOut, B] = outer.forceR(this)(fa)(fb)
    override def onCancel[A](fa: CopyOutIO[A], fin: CopyOutIO[Unit]): Kleisli[M, PGCopyOut, A] = outer.onCancel(this)(fa, fin)
    override def evalOn[A](fa: CopyOutIO[A], ec: ExecutionContext): Kleisli[M, PGCopyOut, A] = outer.evalOn(this)(fa, ec)
    override def async[A](k: (Either[Throwable, A] => Unit) => CopyOutIO[Option[CopyOutIO[Unit]]]) = outer.async(this)(k)

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

  trait FastpathInterpreter extends FastpathOp.Visitor[Kleisli[M, PGFastpath, *]] {

    // common operations delegate to outer interpreter
    override def delay[A](thunk: => A) = outer.delay(thunk)
    override def raw[A](f: PGFastpath => A) = outer.raw(f)
    override def embed[A](e: Embedded[A]) = outer.embed(e)
    override def raiseError[A](e: Throwable): Kleisli[M, PGFastpath, A] = outer.raiseError(e)
    override def monotonic: Kleisli[M, PGFastpath, FiniteDuration] = outer.monotonic
    override def realTime: Kleisli[M, PGFastpath, FiniteDuration] = outer.realTime
    override def suspend[A](hint: Sync.Type)(thunk: => A): Kleisli[M, PGFastpath, A] = outer.suspend(hint)(thunk)
    override def canceled: Kleisli[M, PGFastpath, Unit] = outer.canceled
    override def cede: Kleisli[M, PGFastpath, Unit] = outer.cede
    override def sleep(time: FiniteDuration): Kleisli[M, PGFastpath, Unit] = outer.sleep(time)
    override def executionContext: Kleisli[M, PGFastpath, ExecutionContext] = outer.executionContext
    
    // for operations using FastpathIO we must call ourself recursively
    override def handleErrorWith[A](fa: FastpathIO[A])(f: Throwable => FastpathIO[A]): Kleisli[M, PGFastpath, A] = outer.handleErrorWith(this)(fa)(f)
    override def forceR[A, B](fa: FastpathIO[A])(fb: FastpathIO[B]): Kleisli[M, PGFastpath, B] = outer.forceR(this)(fa)(fb)
    override def onCancel[A](fa: FastpathIO[A], fin: FastpathIO[Unit]): Kleisli[M, PGFastpath, A] = outer.onCancel(this)(fa, fin)
    override def evalOn[A](fa: FastpathIO[A], ec: ExecutionContext): Kleisli[M, PGFastpath, A] = outer.evalOn(this)(fa, ec)
    override def async[A](k: (Either[Throwable, A] => Unit) => FastpathIO[Option[FastpathIO[Unit]]]) = outer.async(this)(k)

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

  trait LargeObjectInterpreter extends LargeObjectOp.Visitor[Kleisli[M, LargeObject, *]] {

    // common operations delegate to outer interpreter
    override def delay[A](thunk: => A) = outer.delay(thunk)
    override def raw[A](f: LargeObject => A) = outer.raw(f)
    override def embed[A](e: Embedded[A]) = outer.embed(e)
    override def raiseError[A](e: Throwable): Kleisli[M, LargeObject, A] = outer.raiseError(e)
    override def monotonic: Kleisli[M, LargeObject, FiniteDuration] = outer.monotonic
    override def realTime: Kleisli[M, LargeObject, FiniteDuration] = outer.realTime
    override def suspend[A](hint: Sync.Type)(thunk: => A): Kleisli[M, LargeObject, A] = outer.suspend(hint)(thunk)
    override def canceled: Kleisli[M, LargeObject, Unit] = outer.canceled
    override def cede: Kleisli[M, LargeObject, Unit] = outer.cede
    override def sleep(time: FiniteDuration): Kleisli[M, LargeObject, Unit] = outer.sleep(time)
    override def executionContext: Kleisli[M, LargeObject, ExecutionContext] = outer.executionContext
    
    // for operations using LargeObjectIO we must call ourself recursively
    override def handleErrorWith[A](fa: LargeObjectIO[A])(f: Throwable => LargeObjectIO[A]): Kleisli[M, LargeObject, A] = outer.handleErrorWith(this)(fa)(f)
    override def forceR[A, B](fa: LargeObjectIO[A])(fb: LargeObjectIO[B]): Kleisli[M, LargeObject, B] = outer.forceR(this)(fa)(fb)
    override def onCancel[A](fa: LargeObjectIO[A], fin: LargeObjectIO[Unit]): Kleisli[M, LargeObject, A] = outer.onCancel(this)(fa, fin)
    override def evalOn[A](fa: LargeObjectIO[A], ec: ExecutionContext): Kleisli[M, LargeObject, A] = outer.evalOn(this)(fa, ec)
    override def async[A](k: (Either[Throwable, A] => Unit) => LargeObjectIO[Option[LargeObjectIO[Unit]]]) = outer.async(this)(k)

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

  trait LargeObjectManagerInterpreter extends LargeObjectManagerOp.Visitor[Kleisli[M, LargeObjectManager, *]] {

    // common operations delegate to outer interpreter
    override def delay[A](thunk: => A) = outer.delay(thunk)
    override def raw[A](f: LargeObjectManager => A) = outer.raw(f)
    override def embed[A](e: Embedded[A]) = outer.embed(e)
    override def raiseError[A](e: Throwable): Kleisli[M, LargeObjectManager, A] = outer.raiseError(e)
    override def monotonic: Kleisli[M, LargeObjectManager, FiniteDuration] = outer.monotonic
    override def realTime: Kleisli[M, LargeObjectManager, FiniteDuration] = outer.realTime
    override def suspend[A](hint: Sync.Type)(thunk: => A): Kleisli[M, LargeObjectManager, A] = outer.suspend(hint)(thunk)
    override def canceled: Kleisli[M, LargeObjectManager, Unit] = outer.canceled
    override def cede: Kleisli[M, LargeObjectManager, Unit] = outer.cede
    override def sleep(time: FiniteDuration): Kleisli[M, LargeObjectManager, Unit] = outer.sleep(time)
    override def executionContext: Kleisli[M, LargeObjectManager, ExecutionContext] = outer.executionContext
    
    // for operations using LargeObjectManagerIO we must call ourself recursively
    override def handleErrorWith[A](fa: LargeObjectManagerIO[A])(f: Throwable => LargeObjectManagerIO[A]): Kleisli[M, LargeObjectManager, A] = outer.handleErrorWith(this)(fa)(f)
    override def forceR[A, B](fa: LargeObjectManagerIO[A])(fb: LargeObjectManagerIO[B]): Kleisli[M, LargeObjectManager, B] = outer.forceR(this)(fa)(fb)
    override def onCancel[A](fa: LargeObjectManagerIO[A], fin: LargeObjectManagerIO[Unit]): Kleisli[M, LargeObjectManager, A] = outer.onCancel(this)(fa, fin)
    override def evalOn[A](fa: LargeObjectManagerIO[A], ec: ExecutionContext): Kleisli[M, LargeObjectManager, A] = outer.evalOn(this)(fa, ec)
    override def async[A](k: (Either[Throwable, A] => Unit) => LargeObjectManagerIO[Option[LargeObjectManagerIO[Unit]]]) = outer.async(this)(k)

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

  trait PGConnectionInterpreter extends PGConnectionOp.Visitor[Kleisli[M, PGConnection, *]] {

    // common operations delegate to outer interpreter
    override def delay[A](thunk: => A) = outer.delay(thunk)
    override def raw[A](f: PGConnection => A) = outer.raw(f)
    override def embed[A](e: Embedded[A]) = outer.embed(e)
    override def raiseError[A](e: Throwable): Kleisli[M, PGConnection, A] = outer.raiseError(e)
    override def monotonic: Kleisli[M, PGConnection, FiniteDuration] = outer.monotonic
    override def realTime: Kleisli[M, PGConnection, FiniteDuration] = outer.realTime
    override def suspend[A](hint: Sync.Type)(thunk: => A): Kleisli[M, PGConnection, A] = outer.suspend(hint)(thunk)
    override def canceled: Kleisli[M, PGConnection, Unit] = outer.canceled
    override def cede: Kleisli[M, PGConnection, Unit] = outer.cede
    override def sleep(time: FiniteDuration): Kleisli[M, PGConnection, Unit] = outer.sleep(time)
    override def executionContext: Kleisli[M, PGConnection, ExecutionContext] = outer.executionContext
    
    // for operations using PGConnectionIO we must call ourself recursively
    override def handleErrorWith[A](fa: PGConnectionIO[A])(f: Throwable => PGConnectionIO[A]): Kleisli[M, PGConnection, A] = outer.handleErrorWith(this)(fa)(f)
    override def forceR[A, B](fa: PGConnectionIO[A])(fb: PGConnectionIO[B]): Kleisli[M, PGConnection, B] = outer.forceR(this)(fa)(fb)
    override def onCancel[A](fa: PGConnectionIO[A], fin: PGConnectionIO[Unit]): Kleisli[M, PGConnection, A] = outer.onCancel(this)(fa, fin)
    override def evalOn[A](fa: PGConnectionIO[A], ec: ExecutionContext): Kleisli[M, PGConnection, A] = outer.evalOn(this)(fa, ec)
    override def async[A](k: (Either[Throwable, A] => Unit) => PGConnectionIO[Option[PGConnectionIO[Unit]]]) = outer.async(this)(k)

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
    override def getParameterStatus(a: String) = primitive(_.getParameterStatus(a))
    override def getParameterStatuses = primitive(_.getParameterStatuses)
    override def getPreferQueryMode = primitive(_.getPreferQueryMode)
    override def getPrepareThreshold = primitive(_.getPrepareThreshold)
    override def getReplicationAPI = primitive(_.getReplicationAPI)
    override def setAutosave(a: AutoSave) = primitive(_.setAutosave(a))
    override def setDefaultFetchSize(a: Int) = primitive(_.setDefaultFetchSize(a))
    override def setPrepareThreshold(a: Int) = primitive(_.setPrepareThreshold(a))

  }


}

