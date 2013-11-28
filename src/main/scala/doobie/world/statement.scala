package doobie
package world

import doobie.util._
import doobie.JdbcType
import java.sql.PreparedStatement
import scalaz._
import Scalaz._

object statement extends DWorld.Indexed {

  protected type R = PreparedStatement

  ////// PRIMITIVE OPS

  def setN[A](n: Int, a:A)(implicit A: Primitive[A]): Action[Unit] =
    asks(A.set(_)(n, a)) :++> s"SET $n $a"

  def setNullN[A](n: Int)(implicit A: Primitive[A]): Action[Unit] =
    asks(_.setNull(n, A.jdbcType.toInt)) :++> s"SET $n NULL"

  ////// INDEXED OPS

  def advance: Action[Unit] =
    mod(_ + 1)

  def set[A: Primitive](a: A): Action[Unit] =
    get >>= (n => setN(n, a))

  def setNull[A: Primitive]: Action[Unit] =
    get >>= (n => setNullN(n))

  ////// EXECUTION

  def execute: Action[Unit] =
    asks(_.execute).void :++> "EXECUTE"

  def executeUpdate: Action[Int] =
    asks(_.executeUpdate) :++> "EXECUTE UPDATE"

  ////// LIFTING INTO CONNECTION WORLD

  def lift[A](sql: String, a: Action[A]): connection.Action[A] =
    connection.prepare(sql, runi(_, a))

  // syntax
  implicit class StatementOps[A](a: Action[A]) {
    def lift(sql: String): connection.Action[A] =
      statement.lift(sql, a)
  }

}


