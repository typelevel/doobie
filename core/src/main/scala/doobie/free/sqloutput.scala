package doobie.free

import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._
import doobie.free.kleislitrans._

import java.io.InputStream
import java.io.Reader
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
import java.sql.Struct
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
 * Algebra and free monad for primitive operations over a `java.sql.SQLOutput`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `SQLOutputIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `SQLOutputOp` to another monad via
 * `Free#foldMap`.
 *
 * The library provides a natural transformation to `Kleisli[M, SQLOutput, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: SQLOutputIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: SQLOutput = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object sqloutput {
  
  /** 
   * Sum type of primitive operations over a `java.sql.SQLOutput`.
   * @group Algebra 
   */
  sealed trait SQLOutputOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: SQLOutput => A): Kleisli[M, SQLOutput, A] = 
      Kleisli((s: SQLOutput) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, SQLOutput, A]
  }

  /** 
   * Module of constructors for `SQLOutputOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `sqloutput` module.
   * @group Algebra 
   */
  object SQLOutputOp {
    
    // This algebra has a default interpreter
    implicit val SQLOutputKleisliTrans: KleisliTrans.Aux[SQLOutputOp, SQLOutput] =
      new KleisliTrans[SQLOutputOp] {
        type J = SQLOutput
        def interpK[M[_]: Monad: Catchable: Capture]: SQLOutputOp ~> Kleisli[M, SQLOutput, ?] =
          new (SQLOutputOp ~> Kleisli[M, SQLOutput, ?]) {
            def apply[A](op: SQLOutputOp[A]): Kleisli[M, SQLOutput, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
    }

    // Combinators
    case class Attempt[A](action: SQLOutputIO[A]) extends SQLOutputOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, SQLOutput, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: SQLOutput => A) extends SQLOutputOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case class  WriteArray(a: SqlArray) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeArray(a))
    }
    case class  WriteAsciiStream(a: InputStream) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeAsciiStream(a))
    }
    case class  WriteBigDecimal(a: BigDecimal) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeBigDecimal(a))
    }
    case class  WriteBinaryStream(a: InputStream) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeBinaryStream(a))
    }
    case class  WriteBlob(a: Blob) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeBlob(a))
    }
    case class  WriteBoolean(a: Boolean) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeBoolean(a))
    }
    case class  WriteByte(a: Byte) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeByte(a))
    }
    case class  WriteBytes(a: Array[Byte]) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeBytes(a))
    }
    case class  WriteCharacterStream(a: Reader) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeCharacterStream(a))
    }
    case class  WriteClob(a: Clob) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeClob(a))
    }
    case class  WriteDate(a: Date) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeDate(a))
    }
    case class  WriteDouble(a: Double) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeDouble(a))
    }
    case class  WriteFloat(a: Float) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeFloat(a))
    }
    case class  WriteInt(a: Int) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeInt(a))
    }
    case class  WriteLong(a: Long) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeLong(a))
    }
    case class  WriteNClob(a: NClob) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeNClob(a))
    }
    case class  WriteNString(a: String) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeNString(a))
    }
    case class  WriteObject(a: SQLData) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeObject(a))
    }
    case class  WriteRef(a: Ref) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeRef(a))
    }
    case class  WriteRowId(a: RowId) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeRowId(a))
    }
    case class  WriteSQLXML(a: SQLXML) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeSQLXML(a))
    }
    case class  WriteShort(a: Short) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeShort(a))
    }
    case class  WriteString(a: String) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeString(a))
    }
    case class  WriteStruct(a: Struct) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeStruct(a))
    }
    case class  WriteTime(a: Time) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeTime(a))
    }
    case class  WriteTimestamp(a: Timestamp) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeTimestamp(a))
    }
    case class  WriteURL(a: URL) extends SQLOutputOp[Unit] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.writeURL(a))
    }

  }
  import SQLOutputOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[SQLOutputOp]]; abstractly, a computation that consumes 
   * a `java.sql.SQLOutput` and produces a value of type `A`. 
   * @group Algebra 
   */
  type SQLOutputIO[A] = F[SQLOutputOp, A]

  /**
   * Catchable instance for [[SQLOutputIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableSQLOutputIO: Catchable[SQLOutputIO] =
    new Catchable[SQLOutputIO] {
      def attempt[A](f: SQLOutputIO[A]): SQLOutputIO[Throwable \/ A] = sqloutput.attempt(f)
      def fail[A](err: Throwable): SQLOutputIO[A] = sqloutput.delay(throw err)
    }

  /**
   * Capture instance for [[SQLOutputIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureSQLOutputIO: Capture[SQLOutputIO] =
    new Capture[SQLOutputIO] {
      def apply[A](a: => A): SQLOutputIO[A] = sqloutput.delay(a)
    }

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): SQLOutputIO[A] =
    F.liftF(Lift(j, action, mod))

  /** 
   * Lift a SQLOutputIO[A] into an exception-capturing SQLOutputIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: SQLOutputIO[A]): SQLOutputIO[Throwable \/ A] =
    F.liftF[SQLOutputOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): SQLOutputIO[A] =
    F.liftF(Pure(a _))

  /**
   * Backdoor for arbitrary computations on the underlying SQLOutput.
   * @group Constructors (Lifting)
   */
  def raw[A](f: SQLOutput => A): SQLOutputIO[A] =
    F.liftF(Raw(f))

  /** 
   * @group Constructors (Primitives)
   */
  def writeArray(a: SqlArray): SQLOutputIO[Unit] =
    F.liftF(WriteArray(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeAsciiStream(a: InputStream): SQLOutputIO[Unit] =
    F.liftF(WriteAsciiStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeBigDecimal(a: BigDecimal): SQLOutputIO[Unit] =
    F.liftF(WriteBigDecimal(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeBinaryStream(a: InputStream): SQLOutputIO[Unit] =
    F.liftF(WriteBinaryStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeBlob(a: Blob): SQLOutputIO[Unit] =
    F.liftF(WriteBlob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeBoolean(a: Boolean): SQLOutputIO[Unit] =
    F.liftF(WriteBoolean(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeByte(a: Byte): SQLOutputIO[Unit] =
    F.liftF(WriteByte(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeBytes(a: Array[Byte]): SQLOutputIO[Unit] =
    F.liftF(WriteBytes(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeCharacterStream(a: Reader): SQLOutputIO[Unit] =
    F.liftF(WriteCharacterStream(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeClob(a: Clob): SQLOutputIO[Unit] =
    F.liftF(WriteClob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeDate(a: Date): SQLOutputIO[Unit] =
    F.liftF(WriteDate(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeDouble(a: Double): SQLOutputIO[Unit] =
    F.liftF(WriteDouble(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeFloat(a: Float): SQLOutputIO[Unit] =
    F.liftF(WriteFloat(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeInt(a: Int): SQLOutputIO[Unit] =
    F.liftF(WriteInt(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeLong(a: Long): SQLOutputIO[Unit] =
    F.liftF(WriteLong(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeNClob(a: NClob): SQLOutputIO[Unit] =
    F.liftF(WriteNClob(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeNString(a: String): SQLOutputIO[Unit] =
    F.liftF(WriteNString(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeObject(a: SQLData): SQLOutputIO[Unit] =
    F.liftF(WriteObject(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeRef(a: Ref): SQLOutputIO[Unit] =
    F.liftF(WriteRef(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeRowId(a: RowId): SQLOutputIO[Unit] =
    F.liftF(WriteRowId(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeSQLXML(a: SQLXML): SQLOutputIO[Unit] =
    F.liftF(WriteSQLXML(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeShort(a: Short): SQLOutputIO[Unit] =
    F.liftF(WriteShort(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeString(a: String): SQLOutputIO[Unit] =
    F.liftF(WriteString(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeStruct(a: Struct): SQLOutputIO[Unit] =
    F.liftF(WriteStruct(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeTime(a: Time): SQLOutputIO[Unit] =
    F.liftF(WriteTime(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeTimestamp(a: Timestamp): SQLOutputIO[Unit] =
    F.liftF(WriteTimestamp(a))

  /** 
   * @group Constructors (Primitives)
   */
  def writeURL(a: URL): SQLOutputIO[Unit] =
    F.liftF(WriteURL(a))

 /** 
  * Natural transformation from `SQLOutputOp` to `Kleisli` for the given `M`, consuming a `java.sql.SQLOutput`. 
  * @group Algebra
  */
  def interpK[M[_]: Monad: Catchable: Capture]: SQLOutputOp ~> Kleisli[M, SQLOutput, ?] =
   SQLOutputOp.SQLOutputKleisliTrans.interpK

 /** 
  * Natural transformation from `SQLOutputIO` to `Kleisli` for the given `M`, consuming a `java.sql.SQLOutput`. 
  * @group Algebra
  */
  def transK[M[_]: Monad: Catchable: Capture]: SQLOutputIO ~> Kleisli[M, SQLOutput, ?] =
   SQLOutputOp.SQLOutputKleisliTrans.transK

 /** 
  * Natural transformation from `SQLOutputIO` to `M`, given a `java.sql.SQLOutput`. 
  * @group Algebra
  */
 def trans[M[_]: Monad: Catchable: Capture](c: SQLOutput): SQLOutputIO ~> M =
   SQLOutputOp.SQLOutputKleisliTrans.trans[M](c)

  /**
   * Syntax for `SQLOutputIO`.
   * @group Algebra
   */
  implicit class SQLOutputIOOps[A](ma: SQLOutputIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, SQLOutput, A] =
      SQLOutputOp.SQLOutputKleisliTrans.transK[M].apply(ma)
  }

}

