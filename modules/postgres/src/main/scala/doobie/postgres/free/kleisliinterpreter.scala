// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.free

// Library imports
import cats.~>
import cats.data.Kleisli
import cats.effect.{ Async, Blocker, ContextShift, ExitCase }
import cats.implicits._
import cats.effect.implicits._
import doobie.util.Sketch.sketch
import scala.concurrent.ExecutionContext
import com.github.ghik.silencer.silent
import io.chrisdavenport.log4cats.extras.LogLevel
import io.chrisdavenport.log4cats.{ Logger => C4JLogger }

// Types referenced in the JDBC API
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.lang.Class
import java.lang.String
import java.sql.ResultSet
import java.sql.{ Array => SqlArray }
import java.util.Map
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

  def apply[M[_]](b: Blocker)(
    implicit am: Async[M],
             cs: ContextShift[M]
  ): KleisliInterpreter[M] =
    new KleisliInterpreter[M] {
      lazy val asyncM = am
      lazy val contextShiftM = cs
      lazy val blocker = b
    }

}

// Family of interpreters into Kleisli arrows for some monad M.
@silent("deprecated")
trait KleisliInterpreter[M[_]] { outer =>

  implicit val asyncM: Async[M]

  // We need these things in order to provide ContextShift[ConnectionIO] and so on, and also
  // to support shifting blocking operations to another pool.
  implicit val contextShiftM: ContextShift[M]
  val blocker: Blocker

  // The 7 interpreters, with definitions below. These can be overridden to customize behavior.
  lazy val CopyInInterpreter: CopyInOp ~> Kleisli[M, Env[M, PGCopyIn], ?] = new CopyInInterpreter { }
  lazy val CopyManagerInterpreter: CopyManagerOp ~> Kleisli[M, Env[M, PGCopyManager], ?] = new CopyManagerInterpreter { }
  lazy val CopyOutInterpreter: CopyOutOp ~> Kleisli[M, Env[M, PGCopyOut], ?] = new CopyOutInterpreter { }
  lazy val FastpathInterpreter: FastpathOp ~> Kleisli[M, Env[M, PGFastpath], ?] = new FastpathInterpreter { }
  lazy val LargeObjectInterpreter: LargeObjectOp ~> Kleisli[M, Env[M, LargeObject], ?] = new LargeObjectInterpreter { }
  lazy val LargeObjectManagerInterpreter: LargeObjectManagerOp ~> Kleisli[M, Env[M, LargeObjectManager], ?] = new LargeObjectManagerInterpreter { }
  lazy val PGConnectionInterpreter: PGConnectionOp ~> Kleisli[M, Env[M, PGConnection], ?] = new PGConnectionInterpreter { }

  // Some methods are common to all interpreters and can be overridden to change behavior globally.
  private val now = asyncM.delay(System.currentTimeMillis)
  protected def primitive[J, A](f: J => A, method: String, args: Any*): Kleisli[M, Env[M, J], A] =
    Kleisli { e =>
      blocker.blockOn[M, A] {
        lazy val prefix = s"${sketch(e.jdbc)}.$method(${args.map(sketch).mkString(",")})"
        now.flatMap { t0 =>
          asyncM.delay(f(e.jdbc)).guaranteeCase {
            case ExitCase.Completed => asyncM.unit // we'll handls this case later
            case ExitCase.Canceled  => now.flatMap { t1 => e.logger.info(s"$prefix Canceled ${t1 - t0}ms") }
            case ExitCase.Error(t)  => now.flatMap { t1 => e.logger.info(t)(s"$prefix Error ${t1 - t0}ms") }
          } .flatTap { a =>
            now.flatMap { t1 => e.logger.info(s"$prefix Completed ${t1 - t0}ms ${sketch(a)}") }
          }
        }
      }
    }

  def delay[J, A](a: () => A): Kleisli[M, Env[M, J], A] = Kleisli(_ => asyncM.delay(a()))
  def raw[J, A](f: J => A): Kleisli[M, Env[M, J], A] = primitive(f, "raw") // for now
  def raiseError[J, A](e: Throwable): Kleisli[M, Env[M, J], A] = Kleisli(_ => asyncM.raiseError(e))
  def async[J, A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, Env[M, J], A] = Kleisli(_ => asyncM.async(k))
  def embed[J, A](e: Embedded[A]): Kleisli[M, Env[M, J], A] =
    e match {
      case Embedded.CopyIn(j, fa) => Kleisli { case env => fa.foldMap(CopyInInterpreter).run(env.copy(jdbc = j)) }
      case Embedded.CopyManager(j, fa) => Kleisli { case env => fa.foldMap(CopyManagerInterpreter).run(env.copy(jdbc = j)) }
      case Embedded.CopyOut(j, fa) => Kleisli { case env => fa.foldMap(CopyOutInterpreter).run(env.copy(jdbc = j)) }
      case Embedded.Fastpath(j, fa) => Kleisli { case env => fa.foldMap(FastpathInterpreter).run(env.copy(jdbc = j)) }
      case Embedded.LargeObject(j, fa) => Kleisli { case env => fa.foldMap(LargeObjectInterpreter).run(env.copy(jdbc = j)) }
      case Embedded.LargeObjectManager(j, fa) => Kleisli { case env => fa.foldMap(LargeObjectManagerInterpreter).run(env.copy(jdbc = j)) }
      case Embedded.PGConnection(j, fa) => Kleisli { case env => fa.foldMap(PGConnectionInterpreter).run(env.copy(jdbc = j)) }
    }

  // Interpreters
  trait CopyInInterpreter extends CopyInOp.Visitor[Kleisli[M, Env[M, PGCopyIn], ?]] {

    // common operations delegate to outer interpreter
    override def raw[A](f: PGCopyIn => A): Kleisli[M, Env[M, PGCopyIn], A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, Env[M, PGCopyIn], A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, Env[M, PGCopyIn], A] = outer.delay(a)
    override def raiseError[A](err: Throwable): Kleisli[M, Env[M, PGCopyIn], A] = outer.raiseError(err)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, Env[M, PGCopyIn], A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => CopyInIO[Unit]): Kleisli[M, Env[M, PGCopyIn], A] =
      Kleisli(j => asyncM.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: CopyInIO[A], f: Throwable => CopyInIO[A]): Kleisli[M, Env[M, PGCopyIn], A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        asyncM.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: CopyInIO[A])(use: A => CopyInIO[B])(release: (A, ExitCase[Throwable]) => CopyInIO[Unit]): Kleisli[M, Env[M, PGCopyIn], B] =
      Kleisli(j => asyncM.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    val shift: Kleisli[M, Env[M, PGCopyIn], Unit] =
      Kleisli(_ => contextShiftM.shift)

    def evalOn[A](ec: ExecutionContext)(fa: CopyInIO[A]): Kleisli[M, Env[M, PGCopyIn], A] =
      Kleisli(j => contextShiftM.evalOn(ec)(fa.foldMap(this).run(j)))

    def log(level: LogLevel, throwable: Option[Throwable], message: => String): Kleisli[M, Env[M, PGCopyIn], Unit] =
      Kleisli { _ =>
        (level, throwable) match {

          case (LogLevel.Error, None)    => asyncM.pure(())
          case (LogLevel.Warn,  None)    => asyncM.pure(())
          case (LogLevel.Info,  None)    => asyncM.pure(())
          case (LogLevel.Debug, None)    => asyncM.pure(())
          case (LogLevel.Trace, None)    => asyncM.pure(())

          case (LogLevel.Error, Some(_)) => asyncM.pure(())
          case (LogLevel.Warn,  Some(_)) => asyncM.pure(())
          case (LogLevel.Info,  Some(_)) => asyncM.pure(())
          case (LogLevel.Debug, Some(_)) => asyncM.pure(())
          case (LogLevel.Trace, Some(_)) => asyncM.pure(())

        }
      }

    // domain-specific operations are implemented in terms of `primitive`
    override def cancelCopy = primitive(_.cancelCopy, "cancelCopy")
    override def endCopy = primitive(_.endCopy, "endCopy")
    override def flushCopy = primitive(_.flushCopy, "flushCopy")
    override def getFieldCount = primitive(_.getFieldCount, "getFieldCount")
    override def getFieldFormat(a: Int) = primitive(_.getFieldFormat(a), "getFieldFormat", a: Int)
    override def getFormat = primitive(_.getFormat, "getFormat")
    override def getHandledRowCount = primitive(_.getHandledRowCount, "getHandledRowCount")
    override def isActive = primitive(_.isActive, "isActive")
    override def writeToCopy(a: Array[Byte], b: Int, c: Int) = primitive(_.writeToCopy(a, b, c), "writeToCopy", a: Array[Byte], b: Int, c: Int)

  }

  trait CopyManagerInterpreter extends CopyManagerOp.Visitor[Kleisli[M, Env[M, PGCopyManager], ?]] {

    // common operations delegate to outer interpreter
    override def raw[A](f: PGCopyManager => A): Kleisli[M, Env[M, PGCopyManager], A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, Env[M, PGCopyManager], A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, Env[M, PGCopyManager], A] = outer.delay(a)
    override def raiseError[A](err: Throwable): Kleisli[M, Env[M, PGCopyManager], A] = outer.raiseError(err)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, Env[M, PGCopyManager], A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => CopyManagerIO[Unit]): Kleisli[M, Env[M, PGCopyManager], A] =
      Kleisli(j => asyncM.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: CopyManagerIO[A], f: Throwable => CopyManagerIO[A]): Kleisli[M, Env[M, PGCopyManager], A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        asyncM.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: CopyManagerIO[A])(use: A => CopyManagerIO[B])(release: (A, ExitCase[Throwable]) => CopyManagerIO[Unit]): Kleisli[M, Env[M, PGCopyManager], B] =
      Kleisli(j => asyncM.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    val shift: Kleisli[M, Env[M, PGCopyManager], Unit] =
      Kleisli(_ => contextShiftM.shift)

    def evalOn[A](ec: ExecutionContext)(fa: CopyManagerIO[A]): Kleisli[M, Env[M, PGCopyManager], A] =
      Kleisli(j => contextShiftM.evalOn(ec)(fa.foldMap(this).run(j)))

    def log(level: LogLevel, throwable: Option[Throwable], message: => String): Kleisli[M, Env[M, PGCopyManager], Unit] =
      Kleisli { _ =>
        (level, throwable) match {

          case (LogLevel.Error, None)    => asyncM.pure(())
          case (LogLevel.Warn,  None)    => asyncM.pure(())
          case (LogLevel.Info,  None)    => asyncM.pure(())
          case (LogLevel.Debug, None)    => asyncM.pure(())
          case (LogLevel.Trace, None)    => asyncM.pure(())

          case (LogLevel.Error, Some(_)) => asyncM.pure(())
          case (LogLevel.Warn,  Some(_)) => asyncM.pure(())
          case (LogLevel.Info,  Some(_)) => asyncM.pure(())
          case (LogLevel.Debug, Some(_)) => asyncM.pure(())
          case (LogLevel.Trace, Some(_)) => asyncM.pure(())

        }
      }

    // domain-specific operations are implemented in terms of `primitive`
    override def copyDual(a: String) = primitive(_.copyDual(a), "copyDual", a: String)
    override def copyIn(a: String) = primitive(_.copyIn(a), "copyIn", a: String)
    override def copyIn(a: String, b: InputStream) = primitive(_.copyIn(a, b), "copyIn", a: String, b: InputStream)
    override def copyIn(a: String, b: InputStream, c: Int) = primitive(_.copyIn(a, b, c), "copyIn", a: String, b: InputStream, c: Int)
    override def copyIn(a: String, b: Reader) = primitive(_.copyIn(a, b), "copyIn", a: String, b: Reader)
    override def copyIn(a: String, b: Reader, c: Int) = primitive(_.copyIn(a, b, c), "copyIn", a: String, b: Reader, c: Int)
    override def copyOut(a: String) = primitive(_.copyOut(a), "copyOut", a: String)
    override def copyOut(a: String, b: OutputStream) = primitive(_.copyOut(a, b), "copyOut", a: String, b: OutputStream)
    override def copyOut(a: String, b: Writer) = primitive(_.copyOut(a, b), "copyOut", a: String, b: Writer)

  }

  trait CopyOutInterpreter extends CopyOutOp.Visitor[Kleisli[M, Env[M, PGCopyOut], ?]] {

    // common operations delegate to outer interpreter
    override def raw[A](f: PGCopyOut => A): Kleisli[M, Env[M, PGCopyOut], A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, Env[M, PGCopyOut], A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, Env[M, PGCopyOut], A] = outer.delay(a)
    override def raiseError[A](err: Throwable): Kleisli[M, Env[M, PGCopyOut], A] = outer.raiseError(err)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, Env[M, PGCopyOut], A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => CopyOutIO[Unit]): Kleisli[M, Env[M, PGCopyOut], A] =
      Kleisli(j => asyncM.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: CopyOutIO[A], f: Throwable => CopyOutIO[A]): Kleisli[M, Env[M, PGCopyOut], A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        asyncM.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: CopyOutIO[A])(use: A => CopyOutIO[B])(release: (A, ExitCase[Throwable]) => CopyOutIO[Unit]): Kleisli[M, Env[M, PGCopyOut], B] =
      Kleisli(j => asyncM.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    val shift: Kleisli[M, Env[M, PGCopyOut], Unit] =
      Kleisli(_ => contextShiftM.shift)

    def evalOn[A](ec: ExecutionContext)(fa: CopyOutIO[A]): Kleisli[M, Env[M, PGCopyOut], A] =
      Kleisli(j => contextShiftM.evalOn(ec)(fa.foldMap(this).run(j)))

    def log(level: LogLevel, throwable: Option[Throwable], message: => String): Kleisli[M, Env[M, PGCopyOut], Unit] =
      Kleisli { _ =>
        (level, throwable) match {

          case (LogLevel.Error, None)    => asyncM.pure(())
          case (LogLevel.Warn,  None)    => asyncM.pure(())
          case (LogLevel.Info,  None)    => asyncM.pure(())
          case (LogLevel.Debug, None)    => asyncM.pure(())
          case (LogLevel.Trace, None)    => asyncM.pure(())

          case (LogLevel.Error, Some(_)) => asyncM.pure(())
          case (LogLevel.Warn,  Some(_)) => asyncM.pure(())
          case (LogLevel.Info,  Some(_)) => asyncM.pure(())
          case (LogLevel.Debug, Some(_)) => asyncM.pure(())
          case (LogLevel.Trace, Some(_)) => asyncM.pure(())

        }
      }

    // domain-specific operations are implemented in terms of `primitive`
    override def cancelCopy = primitive(_.cancelCopy, "cancelCopy")
    override def getFieldCount = primitive(_.getFieldCount, "getFieldCount")
    override def getFieldFormat(a: Int) = primitive(_.getFieldFormat(a), "getFieldFormat", a: Int)
    override def getFormat = primitive(_.getFormat, "getFormat")
    override def getHandledRowCount = primitive(_.getHandledRowCount, "getHandledRowCount")
    override def isActive = primitive(_.isActive, "isActive")
    override def readFromCopy = primitive(_.readFromCopy, "readFromCopy")
    override def readFromCopy(a: Boolean) = primitive(_.readFromCopy(a), "readFromCopy", a: Boolean)

  }

  trait FastpathInterpreter extends FastpathOp.Visitor[Kleisli[M, Env[M, PGFastpath], ?]] {

    // common operations delegate to outer interpreter
    override def raw[A](f: PGFastpath => A): Kleisli[M, Env[M, PGFastpath], A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, Env[M, PGFastpath], A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, Env[M, PGFastpath], A] = outer.delay(a)
    override def raiseError[A](err: Throwable): Kleisli[M, Env[M, PGFastpath], A] = outer.raiseError(err)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, Env[M, PGFastpath], A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => FastpathIO[Unit]): Kleisli[M, Env[M, PGFastpath], A] =
      Kleisli(j => asyncM.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: FastpathIO[A], f: Throwable => FastpathIO[A]): Kleisli[M, Env[M, PGFastpath], A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        asyncM.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: FastpathIO[A])(use: A => FastpathIO[B])(release: (A, ExitCase[Throwable]) => FastpathIO[Unit]): Kleisli[M, Env[M, PGFastpath], B] =
      Kleisli(j => asyncM.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    val shift: Kleisli[M, Env[M, PGFastpath], Unit] =
      Kleisli(_ => contextShiftM.shift)

    def evalOn[A](ec: ExecutionContext)(fa: FastpathIO[A]): Kleisli[M, Env[M, PGFastpath], A] =
      Kleisli(j => contextShiftM.evalOn(ec)(fa.foldMap(this).run(j)))

    def log(level: LogLevel, throwable: Option[Throwable], message: => String): Kleisli[M, Env[M, PGFastpath], Unit] =
      Kleisli { _ =>
        (level, throwable) match {

          case (LogLevel.Error, None)    => asyncM.pure(())
          case (LogLevel.Warn,  None)    => asyncM.pure(())
          case (LogLevel.Info,  None)    => asyncM.pure(())
          case (LogLevel.Debug, None)    => asyncM.pure(())
          case (LogLevel.Trace, None)    => asyncM.pure(())

          case (LogLevel.Error, Some(_)) => asyncM.pure(())
          case (LogLevel.Warn,  Some(_)) => asyncM.pure(())
          case (LogLevel.Info,  Some(_)) => asyncM.pure(())
          case (LogLevel.Debug, Some(_)) => asyncM.pure(())
          case (LogLevel.Trace, Some(_)) => asyncM.pure(())

        }
      }

    // domain-specific operations are implemented in terms of `primitive`
    override def addFunction(a: String, b: Int) = primitive(_.addFunction(a, b), "addFunction", a: String, b: Int)
    override def addFunctions(a: ResultSet) = primitive(_.addFunctions(a), "addFunctions", a: ResultSet)
    override def fastpath(a: Int, b: Array[FastpathArg]) = primitive(_.fastpath(a, b), "fastpath", a: Int, b: Array[FastpathArg])
    override def fastpath(a: Int, b: Boolean, c: Array[FastpathArg]) = primitive(_.fastpath(a, b, c), "fastpath", a: Int, b: Boolean, c: Array[FastpathArg])
    override def fastpath(a: String, b: Array[FastpathArg]) = primitive(_.fastpath(a, b), "fastpath", a: String, b: Array[FastpathArg])
    override def fastpath(a: String, b: Boolean, c: Array[FastpathArg]) = primitive(_.fastpath(a, b, c), "fastpath", a: String, b: Boolean, c: Array[FastpathArg])
    override def getData(a: String, b: Array[FastpathArg]) = primitive(_.getData(a, b), "getData", a: String, b: Array[FastpathArg])
    override def getID(a: String) = primitive(_.getID(a), "getID", a: String)
    override def getInteger(a: String, b: Array[FastpathArg]) = primitive(_.getInteger(a, b), "getInteger", a: String, b: Array[FastpathArg])
    override def getLong(a: String, b: Array[FastpathArg]) = primitive(_.getLong(a, b), "getLong", a: String, b: Array[FastpathArg])
    override def getOID(a: String, b: Array[FastpathArg]) = primitive(_.getOID(a, b), "getOID", a: String, b: Array[FastpathArg])

  }

  trait LargeObjectInterpreter extends LargeObjectOp.Visitor[Kleisli[M, Env[M, LargeObject], ?]] {

    // common operations delegate to outer interpreter
    override def raw[A](f: LargeObject => A): Kleisli[M, Env[M, LargeObject], A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, Env[M, LargeObject], A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, Env[M, LargeObject], A] = outer.delay(a)
    override def raiseError[A](err: Throwable): Kleisli[M, Env[M, LargeObject], A] = outer.raiseError(err)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, Env[M, LargeObject], A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => LargeObjectIO[Unit]): Kleisli[M, Env[M, LargeObject], A] =
      Kleisli(j => asyncM.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: LargeObjectIO[A], f: Throwable => LargeObjectIO[A]): Kleisli[M, Env[M, LargeObject], A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        asyncM.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: LargeObjectIO[A])(use: A => LargeObjectIO[B])(release: (A, ExitCase[Throwable]) => LargeObjectIO[Unit]): Kleisli[M, Env[M, LargeObject], B] =
      Kleisli(j => asyncM.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    val shift: Kleisli[M, Env[M, LargeObject], Unit] =
      Kleisli(_ => contextShiftM.shift)

    def evalOn[A](ec: ExecutionContext)(fa: LargeObjectIO[A]): Kleisli[M, Env[M, LargeObject], A] =
      Kleisli(j => contextShiftM.evalOn(ec)(fa.foldMap(this).run(j)))

    def log(level: LogLevel, throwable: Option[Throwable], message: => String): Kleisli[M, Env[M, LargeObject], Unit] =
      Kleisli { _ =>
        (level, throwable) match {

          case (LogLevel.Error, None)    => asyncM.pure(())
          case (LogLevel.Warn,  None)    => asyncM.pure(())
          case (LogLevel.Info,  None)    => asyncM.pure(())
          case (LogLevel.Debug, None)    => asyncM.pure(())
          case (LogLevel.Trace, None)    => asyncM.pure(())

          case (LogLevel.Error, Some(_)) => asyncM.pure(())
          case (LogLevel.Warn,  Some(_)) => asyncM.pure(())
          case (LogLevel.Info,  Some(_)) => asyncM.pure(())
          case (LogLevel.Debug, Some(_)) => asyncM.pure(())
          case (LogLevel.Trace, Some(_)) => asyncM.pure(())

        }
      }

    // domain-specific operations are implemented in terms of `primitive`
    override def close = primitive(_.close, "close")
    override def copy = primitive(_.copy, "copy")
    override def getInputStream = primitive(_.getInputStream, "getInputStream")
    override def getInputStream(a: Long) = primitive(_.getInputStream(a), "getInputStream", a: Long)
    override def getLongOID = primitive(_.getLongOID, "getLongOID")
    override def getOID = primitive(_.getOID, "getOID")
    override def getOutputStream = primitive(_.getOutputStream, "getOutputStream")
    override def read(a: Array[Byte], b: Int, c: Int) = primitive(_.read(a, b, c), "read", a: Array[Byte], b: Int, c: Int)
    override def read(a: Int) = primitive(_.read(a), "read", a: Int)
    override def seek(a: Int) = primitive(_.seek(a), "seek", a: Int)
    override def seek(a: Int, b: Int) = primitive(_.seek(a, b), "seek", a: Int, b: Int)
    override def seek64(a: Long, b: Int) = primitive(_.seek64(a, b), "seek64", a: Long, b: Int)
    override def size = primitive(_.size, "size")
    override def size64 = primitive(_.size64, "size64")
    override def tell = primitive(_.tell, "tell")
    override def tell64 = primitive(_.tell64, "tell64")
    override def truncate(a: Int) = primitive(_.truncate(a), "truncate", a: Int)
    override def truncate64(a: Long) = primitive(_.truncate64(a), "truncate64", a: Long)
    override def write(a: Array[Byte]) = primitive(_.write(a), "write", a: Array[Byte])
    override def write(a: Array[Byte], b: Int, c: Int) = primitive(_.write(a, b, c), "write", a: Array[Byte], b: Int, c: Int)

  }

  trait LargeObjectManagerInterpreter extends LargeObjectManagerOp.Visitor[Kleisli[M, Env[M, LargeObjectManager], ?]] {

    // common operations delegate to outer interpreter
    override def raw[A](f: LargeObjectManager => A): Kleisli[M, Env[M, LargeObjectManager], A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, Env[M, LargeObjectManager], A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, Env[M, LargeObjectManager], A] = outer.delay(a)
    override def raiseError[A](err: Throwable): Kleisli[M, Env[M, LargeObjectManager], A] = outer.raiseError(err)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, Env[M, LargeObjectManager], A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => LargeObjectManagerIO[Unit]): Kleisli[M, Env[M, LargeObjectManager], A] =
      Kleisli(j => asyncM.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: LargeObjectManagerIO[A], f: Throwable => LargeObjectManagerIO[A]): Kleisli[M, Env[M, LargeObjectManager], A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        asyncM.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: LargeObjectManagerIO[A])(use: A => LargeObjectManagerIO[B])(release: (A, ExitCase[Throwable]) => LargeObjectManagerIO[Unit]): Kleisli[M, Env[M, LargeObjectManager], B] =
      Kleisli(j => asyncM.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    val shift: Kleisli[M, Env[M, LargeObjectManager], Unit] =
      Kleisli(_ => contextShiftM.shift)

    def evalOn[A](ec: ExecutionContext)(fa: LargeObjectManagerIO[A]): Kleisli[M, Env[M, LargeObjectManager], A] =
      Kleisli(j => contextShiftM.evalOn(ec)(fa.foldMap(this).run(j)))

    def log(level: LogLevel, throwable: Option[Throwable], message: => String): Kleisli[M, Env[M, LargeObjectManager], Unit] =
      Kleisli { _ =>
        (level, throwable) match {

          case (LogLevel.Error, None)    => asyncM.pure(())
          case (LogLevel.Warn,  None)    => asyncM.pure(())
          case (LogLevel.Info,  None)    => asyncM.pure(())
          case (LogLevel.Debug, None)    => asyncM.pure(())
          case (LogLevel.Trace, None)    => asyncM.pure(())

          case (LogLevel.Error, Some(_)) => asyncM.pure(())
          case (LogLevel.Warn,  Some(_)) => asyncM.pure(())
          case (LogLevel.Info,  Some(_)) => asyncM.pure(())
          case (LogLevel.Debug, Some(_)) => asyncM.pure(())
          case (LogLevel.Trace, Some(_)) => asyncM.pure(())

        }
      }

    // domain-specific operations are implemented in terms of `primitive`
    override def create = primitive(_.create, "create")
    override def create(a: Int) = primitive(_.create(a), "create", a: Int)
    override def createLO = primitive(_.createLO, "createLO")
    override def createLO(a: Int) = primitive(_.createLO(a), "createLO", a: Int)
    override def delete(a: Int) = primitive(_.delete(a), "delete", a: Int)
    override def delete(a: Long) = primitive(_.delete(a), "delete", a: Long)
    override def open(a: Int) = primitive(_.open(a), "open", a: Int)
    override def open(a: Int, b: Boolean) = primitive(_.open(a, b), "open", a: Int, b: Boolean)
    override def open(a: Int, b: Int) = primitive(_.open(a, b), "open", a: Int, b: Int)
    override def open(a: Int, b: Int, c: Boolean) = primitive(_.open(a, b, c), "open", a: Int, b: Int, c: Boolean)
    override def open(a: Long) = primitive(_.open(a), "open", a: Long)
    override def open(a: Long, b: Boolean) = primitive(_.open(a, b), "open", a: Long, b: Boolean)
    override def open(a: Long, b: Int) = primitive(_.open(a, b), "open", a: Long, b: Int)
    override def open(a: Long, b: Int, c: Boolean) = primitive(_.open(a, b, c), "open", a: Long, b: Int, c: Boolean)
    override def unlink(a: Int) = primitive(_.unlink(a), "unlink", a: Int)
    override def unlink(a: Long) = primitive(_.unlink(a), "unlink", a: Long)

  }

  trait PGConnectionInterpreter extends PGConnectionOp.Visitor[Kleisli[M, Env[M, PGConnection], ?]] {

    // common operations delegate to outer interpreter
    override def raw[A](f: PGConnection => A): Kleisli[M, Env[M, PGConnection], A] = outer.raw(f)
    override def embed[A](e: Embedded[A]): Kleisli[M, Env[M, PGConnection], A] = outer.embed(e)
    override def delay[A](a: () => A): Kleisli[M, Env[M, PGConnection], A] = outer.delay(a)
    override def raiseError[A](err: Throwable): Kleisli[M, Env[M, PGConnection], A] = outer.raiseError(err)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Kleisli[M, Env[M, PGConnection], A] = outer.async(k)

    // for asyncF we must call ourself recursively
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => PGConnectionIO[Unit]): Kleisli[M, Env[M, PGConnection], A] =
      Kleisli(j => asyncM.asyncF(k.andThen(_.foldMap(this).run(j))))

    // for handleErrorWith we must call ourself recursively
    override def handleErrorWith[A](fa: PGConnectionIO[A], f: Throwable => PGConnectionIO[A]): Kleisli[M, Env[M, PGConnection], A] =
      Kleisli { j =>
        val faʹ = fa.foldMap(this).run(j)
        val fʹ  = f.andThen(_.foldMap(this).run(j))
        asyncM.handleErrorWith(faʹ)(fʹ)
      }

    def bracketCase[A, B](acquire: PGConnectionIO[A])(use: A => PGConnectionIO[B])(release: (A, ExitCase[Throwable]) => PGConnectionIO[Unit]): Kleisli[M, Env[M, PGConnection], B] =
      Kleisli(j => asyncM.bracketCase(acquire.foldMap(this).run(j))(use.andThen(_.foldMap(this).run(j)))((a, e) => release(a, e).foldMap(this).run(j)))

    val shift: Kleisli[M, Env[M, PGConnection], Unit] =
      Kleisli(_ => contextShiftM.shift)

    def evalOn[A](ec: ExecutionContext)(fa: PGConnectionIO[A]): Kleisli[M, Env[M, PGConnection], A] =
      Kleisli(j => contextShiftM.evalOn(ec)(fa.foldMap(this).run(j)))

    def log(level: LogLevel, throwable: Option[Throwable], message: => String): Kleisli[M, Env[M, PGConnection], Unit] =
      Kleisli { _ =>
        (level, throwable) match {

          case (LogLevel.Error, None)    => asyncM.pure(())
          case (LogLevel.Warn,  None)    => asyncM.pure(())
          case (LogLevel.Info,  None)    => asyncM.pure(())
          case (LogLevel.Debug, None)    => asyncM.pure(())
          case (LogLevel.Trace, None)    => asyncM.pure(())

          case (LogLevel.Error, Some(_)) => asyncM.pure(())
          case (LogLevel.Warn,  Some(_)) => asyncM.pure(())
          case (LogLevel.Info,  Some(_)) => asyncM.pure(())
          case (LogLevel.Debug, Some(_)) => asyncM.pure(())
          case (LogLevel.Trace, Some(_)) => asyncM.pure(())

        }
      }

    // domain-specific operations are implemented in terms of `primitive`
    override def addDataType(a: String, b: Class[_ <: org.postgresql.util.PGobject]) = primitive(_.addDataType(a, b), "addDataType", a: String, b: Class[_ <: org.postgresql.util.PGobject])
    override def addDataType(a: String, b: String) = primitive(_.addDataType(a, b), "addDataType", a: String, b: String)
    override def createArrayOf(a: String, b: AnyRef) = primitive(_.createArrayOf(a, b), "createArrayOf", a: String, b: AnyRef)
    override def escapeIdentifier(a: String) = primitive(_.escapeIdentifier(a), "escapeIdentifier", a: String)
    override def escapeLiteral(a: String) = primitive(_.escapeLiteral(a), "escapeLiteral", a: String)
    override def getAutosave = primitive(_.getAutosave, "getAutosave")
    override def getBackendPID = primitive(_.getBackendPID, "getBackendPID")
    override def getCopyAPI = primitive(_.getCopyAPI, "getCopyAPI")
    override def getDefaultFetchSize = primitive(_.getDefaultFetchSize, "getDefaultFetchSize")
    override def getFastpathAPI = primitive(_.getFastpathAPI, "getFastpathAPI")
    override def getLargeObjectAPI = primitive(_.getLargeObjectAPI, "getLargeObjectAPI")
    override def getNotifications = primitive(_.getNotifications, "getNotifications")
    override def getNotifications(a: Int) = primitive(_.getNotifications(a), "getNotifications", a: Int)
    override def getParameterStatus(a: String) = primitive(_.getParameterStatus(a), "getParameterStatus", a: String)
    override def getParameterStatuses = primitive(_.getParameterStatuses, "getParameterStatuses")
    override def getPreferQueryMode = primitive(_.getPreferQueryMode, "getPreferQueryMode")
    override def getPrepareThreshold = primitive(_.getPrepareThreshold, "getPrepareThreshold")
    override def getReplicationAPI = primitive(_.getReplicationAPI, "getReplicationAPI")
    override def setAutosave(a: AutoSave) = primitive(_.setAutosave(a), "setAutosave", a: AutoSave)
    override def setDefaultFetchSize(a: Int) = primitive(_.setDefaultFetchSize(a), "setDefaultFetchSize", a: Int)
    override def setPrepareThreshold(a: Int) = primitive(_.setPrepareThreshold(a), "setPrepareThreshold", a: Int)

  }


}

