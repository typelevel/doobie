package doobie
package world

import doobie.util._
import doobie.JdbcType
import java.sql.{PreparedStatement, ResultSet}
import scalaz._
import Scalaz._

object statement extends RWSFWorld with EventLogging with IndexedState {
  import rwsfops._

  protected type R = PreparedStatement

  /** Set primitive parameter `a` at index `n`. */
  def setN[A](n: Int, a:A)(implicit A: Primitive[A]): Action[Unit] =
    asks(A.set(_)(n, a)) :++> s"SET $n $a"

  /** Set parameter at index `n` to NULL. */
  def setNullN[A](n: Int)(implicit A: Primitive[A]): Action[Unit] =
    asks(_.setNull(n, A.jdbcType.toInt)) :++> s"SET $n NULL"

  /** Set primitive parameter `a` at the current index. */
  def set[A: Primitive](a: A): Action[Unit] =
    get >>= (n => setN(n, a))

  /** Set primitive parameter at the current index to NULL. */
  def setNull[A: Primitive]: Action[Unit] =
    get >>= (n => setNullN(n))

  /** Set a composite parameter `a` at the current index. */
  def setC[C](a: C)(implicit C: Composite[C]): Action[Unit] =
    C.set(a)

  /** Execute the statement. */
  def execute: Action[Unit] =
    asks(_.execute).void :++> "EXECUTE"

  /** Execute the statement as an update, returning the number of affected rows. */
  def executeUpdate: Action[Int] =
    asks(_.executeUpdate) :++> "EXECUTE UPDATE"

  /** Execute a query and return the resultset. */
  private def executeQuery: Action[ResultSet] =
    asks(_.executeQuery) :++>> (rs => s"OPEN $rs")

  /** Close a resultset. */
  private def close(rs: ResultSet): Action[Unit] =
    unit(rs.close) :++> s"CLOSE $rs"

  /** Execute the statement and pass the resultset to the given continuation. */
  private[world] def executeQuery[A](f: ResultSet => (W, Throwable \/ A)): Action[A] =
    fops.resource[ResultSet, A](executeQuery, rs => gosub(f(rs)), close)

  implicit class StatementOps[A](a: Action[A]) {

    /** Lift this action with the associated SQL string into connection world. */
    def run(sql: String): connection.Action[A] =
      connection.prepare(sql, runrw(_, a))
  
  }

}


