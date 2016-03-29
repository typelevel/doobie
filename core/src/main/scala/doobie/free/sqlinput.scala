package doobie.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._
import doobie.free.kleislitrans._

import java.io.InputStream
import java.io.Reader
import java.lang.Object
import java.lang.String
import java.math.BigDecimal
import java.net.URL
import java.sql.Blob
import java.sql.CallableStatement
import java.sql.Clob
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.Date
import java.sql.Driver
import java.sql.NClob
import java.sql.PreparedStatement
import java.sql.Ref
import java.sql.ResultSet
import java.sql.RowId
import java.sql.SQLData
import java.sql.SQLInput
import java.sql.SQLOutput
import java.sql.SQLXML
import java.sql.Statement
import java.sql.Time
import java.sql.Timestamp
import java.sql.{ Array => SqlArray }

import nclob.NClobIO
import blob.BlobIO
import clob.ClobIO
import databasemetadata.DatabaseMetaDataIO
import driver.DriverIO
import ref.RefIO
import sqldata.SQLDataIO
import sqlinput.SQLInputIO
import sqloutput.SQLOutputIO
import connection.ConnectionIO
import statement.StatementIO
import preparedstatement.PreparedStatementIO
import callablestatement.CallableStatementIO
import resultset.ResultSetIO

/**
 * Algebra and free monad for primitive operations over a `java.sql.SQLInput`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `SQLInputIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `SQLInputOp` to another monad via
 * `Free#foldMap`.
 *
 * The library provides a natural transformation to `Kleisli[M, SQLInput, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: SQLInputIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: SQLInput = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object sqlinput {
  
  /** 
   * Sum type of primitive operations over a `java.sql.SQLInput`.
   * @group Algebra 
   */
  sealed trait SQLInputOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: SQLInput => A): Kleisli[M, SQLInput, A] = 
      Kleisli((s: SQLInput) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, SQLInput, A]
  }

  /** 
   * Module of constructors for `SQLInputOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `sqlinput` module.
   * @group Algebra 
   */
  object SQLInputOp {
    
    // This algebra has a default interpreter
    implicit val SQLInputKleisliTrans: KleisliTrans.Aux[SQLInputOp, SQLInput] =
      new KleisliTrans[SQLInputOp] {
        type J = SQLInput
        def interpK[M[_]: Monad: Catchable: Capture]: SQLInputOp ~> Kleisli[M, SQLInput, ?] =
          new (SQLInputOp ~> Kleisli[M, SQLInput, ?]) {
            def apply[A](op: SQLInputOp[A]): Kleisli[M, SQLInput, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends SQLInputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
    }

    // Combinators
    case class Attempt[A](action: SQLInputIO[A]) extends SQLInputOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, SQLInput, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends SQLInputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: SQLInput => A) extends SQLInputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case object ReadArray extends SQLInputOp[SqlArray] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readArray())
    }
    case object ReadAsciiStream extends SQLInputOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readAsciiStream())
    }
    case object ReadBigDecimal extends SQLInputOp[BigDecimal] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readBigDecimal())
    }
    case object ReadBinaryStream extends SQLInputOp[InputStream] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readBinaryStream())
    }
    case object ReadBlob extends SQLInputOp[Blob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readBlob())
    }
    case object ReadBoolean extends SQLInputOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readBoolean())
    }
    case object ReadByte extends SQLInputOp[Byte] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readByte())
    }
    case object ReadBytes extends SQLInputOp[Array[Byte]] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readBytes())
    }
    case object ReadCharacterStream extends SQLInputOp[Reader] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readCharacterStream())
    }
    case object ReadClob extends SQLInputOp[Clob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readClob())
    }
    case object ReadDate extends SQLInputOp[Date] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readDate())
    }
    case object ReadDouble extends SQLInputOp[Double] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readDouble())
    }
    case object ReadFloat extends SQLInputOp[Float] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readFloat())
    }
    case object ReadInt extends SQLInputOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readInt())
    }
    case object ReadLong extends SQLInputOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readLong())
    }
    case object ReadNClob extends SQLInputOp[NClob] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readNClob())
    }
    case object ReadNString extends SQLInputOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readNString())
    }
    case object ReadObject extends SQLInputOp[Object] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readObject())
    }
    case object ReadRef extends SQLInputOp[Ref] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readRef())
    }
    case object ReadRowId extends SQLInputOp[RowId] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readRowId())
    }
    case object ReadSQLXML extends SQLInputOp[SQLXML] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readSQLXML())
    }
    case object ReadShort extends SQLInputOp[Short] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readShort())
    }
    case object ReadString extends SQLInputOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readString())
    }
    case object ReadTime extends SQLInputOp[Time] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readTime())
    }
    case object ReadTimestamp extends SQLInputOp[Timestamp] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readTimestamp())
    }
    case object ReadURL extends SQLInputOp[URL] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.readURL())
    }
    case object WasNull extends SQLInputOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.wasNull())
    }

  }
  import SQLInputOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[SQLInputOp]]; abstractly, a computation that consumes 
   * a `java.sql.SQLInput` and produces a value of type `A`. 
   * @group Algebra 
   */
  type SQLInputIO[A] = F[SQLInputOp, A]

  /**
   * Catchable instance for [[SQLInputIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableSQLInputIO: Catchable[SQLInputIO] =
    new Catchable[SQLInputIO] {
      def attempt[A](f: SQLInputIO[A]): SQLInputIO[Throwable \/ A] = sqlinput.attempt(f)
      def fail[A](err: Throwable): SQLInputIO[A] = sqlinput.delay(throw err)
    }

  /**
   * Capture instance for [[SQLInputIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureSQLInputIO: Capture[SQLInputIO] =
    new Capture[SQLInputIO] {
      def apply[A](a: => A): SQLInputIO[A] = sqlinput.delay(a)
    }

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): SQLInputIO[A] =
    F.liftF(Lift(j, action, mod))

  /** 
   * Lift a SQLInputIO[A] into an exception-capturing SQLInputIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: SQLInputIO[A]): SQLInputIO[Throwable \/ A] =
    F.liftF[SQLInputOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): SQLInputIO[A] =
    F.liftF(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying SQLInput.
   * @group Constructors (Lifting)
   */
  def raw[A](f: SQLInput => A): SQLInputIO[A] =
    F.liftF(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  val readArray: SQLInputIO[SqlArray] =
    F.liftF(ReadArray)

  /** 
   * @group Constructors (Primitives)
   */
  val readAsciiStream: SQLInputIO[InputStream] =
    F.liftF(ReadAsciiStream)

  /** 
   * @group Constructors (Primitives)
   */
  val readBigDecimal: SQLInputIO[BigDecimal] =
    F.liftF(ReadBigDecimal)

  /** 
   * @group Constructors (Primitives)
   */
  val readBinaryStream: SQLInputIO[InputStream] =
    F.liftF(ReadBinaryStream)

  /** 
   * @group Constructors (Primitives)
   */
  val readBlob: SQLInputIO[Blob] =
    F.liftF(ReadBlob)

  /** 
   * @group Constructors (Primitives)
   */
  val readBoolean: SQLInputIO[Boolean] =
    F.liftF(ReadBoolean)

  /** 
   * @group Constructors (Primitives)
   */
  val readByte: SQLInputIO[Byte] =
    F.liftF(ReadByte)

  /** 
   * @group Constructors (Primitives)
   */
  val readBytes: SQLInputIO[Array[Byte]] =
    F.liftF(ReadBytes)

  /** 
   * @group Constructors (Primitives)
   */
  val readCharacterStream: SQLInputIO[Reader] =
    F.liftF(ReadCharacterStream)

  /** 
   * @group Constructors (Primitives)
   */
  val readClob: SQLInputIO[Clob] =
    F.liftF(ReadClob)

  /** 
   * @group Constructors (Primitives)
   */
  val readDate: SQLInputIO[Date] =
    F.liftF(ReadDate)

  /** 
   * @group Constructors (Primitives)
   */
  val readDouble: SQLInputIO[Double] =
    F.liftF(ReadDouble)

  /** 
   * @group Constructors (Primitives)
   */
  val readFloat: SQLInputIO[Float] =
    F.liftF(ReadFloat)

  /** 
   * @group Constructors (Primitives)
   */
  val readInt: SQLInputIO[Int] =
    F.liftF(ReadInt)

  /** 
   * @group Constructors (Primitives)
   */
  val readLong: SQLInputIO[Long] =
    F.liftF(ReadLong)

  /** 
   * @group Constructors (Primitives)
   */
  val readNClob: SQLInputIO[NClob] =
    F.liftF(ReadNClob)

  /** 
   * @group Constructors (Primitives)
   */
  val readNString: SQLInputIO[String] =
    F.liftF(ReadNString)

  /** 
   * @group Constructors (Primitives)
   */
  val readObject: SQLInputIO[Object] =
    F.liftF(ReadObject)

  /** 
   * @group Constructors (Primitives)
   */
  val readRef: SQLInputIO[Ref] =
    F.liftF(ReadRef)

  /** 
   * @group Constructors (Primitives)
   */
  val readRowId: SQLInputIO[RowId] =
    F.liftF(ReadRowId)

  /** 
   * @group Constructors (Primitives)
   */
  val readSQLXML: SQLInputIO[SQLXML] =
    F.liftF(ReadSQLXML)

  /** 
   * @group Constructors (Primitives)
   */
  val readShort: SQLInputIO[Short] =
    F.liftF(ReadShort)

  /** 
   * @group Constructors (Primitives)
   */
  val readString: SQLInputIO[String] =
    F.liftF(ReadString)

  /** 
   * @group Constructors (Primitives)
   */
  val readTime: SQLInputIO[Time] =
    F.liftF(ReadTime)

  /** 
   * @group Constructors (Primitives)
   */
  val readTimestamp: SQLInputIO[Timestamp] =
    F.liftF(ReadTimestamp)

  /** 
   * @group Constructors (Primitives)
   */
  val readURL: SQLInputIO[URL] =
    F.liftF(ReadURL)

  /** 
   * @group Constructors (Primitives)
   */
  val wasNull: SQLInputIO[Boolean] =
    F.liftF(WasNull)

 /** 
  * Natural transformation from `SQLInputOp` to `Kleisli` for the given `M`, consuming a `java.sql.SQLInput`. 
  * @group Algebra
  */
  def interpK[M[_]: Monad: Catchable: Capture]: SQLInputOp ~> Kleisli[M, SQLInput, ?] =
   SQLInputOp.SQLInputKleisliTrans.interpK

 /** 
  * Natural transformation from `SQLInputIO` to `Kleisli` for the given `M`, consuming a `java.sql.SQLInput`. 
  * @group Algebra
  */
  def transK[M[_]: Monad: Catchable: Capture]: SQLInputIO ~> Kleisli[M, SQLInput, ?] =
   SQLInputOp.SQLInputKleisliTrans.transK

 /** 
  * Natural transformation from `SQLInputIO` to `M`, given a `java.sql.SQLInput`. 
  * @group Algebra
  */
 def trans[M[_]: Monad: Catchable: Capture](c: SQLInput): SQLInputIO ~> M =
   SQLInputOp.SQLInputKleisliTrans.trans[M](c)

  /**
   * Syntax for `SQLInputIO`.
   * @group Algebra
   */
  implicit class SQLInputIOOps[A](ma: SQLInputIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, SQLInput, A] =
      SQLInputOp.SQLInputKleisliTrans.transK[M].apply(ma)
  }

}

