package example

import cats._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.free.connection.ConnectionOp

/**
 * Example interpreter that fails if it encounters any `ConnectionOp` constructors. This lets you
 * run `ConnectionIO` programs that rely only on pure values.
 */
object Noop {

  val noop: ConnectionOp ~> Either[String, ?] =
    λ[ConnectionOp ~> Either[String, ?]] { c =>
      Left(s"Can't interpret $c")
    }

  val prog1: ConnectionIO[Int] =
    for {
      a <- 42.pure[ConnectionIO]
      b <-  3.pure[ConnectionIO]
    } yield a + b

  val prog2: ConnectionIO[Int] =
    sql"select 42".query[Int].unique

  def main(args: Array[String]): Unit = {
    println(prog1.foldMap(noop))
    println(prog2.foldMap(noop))
  }

}