// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import doobie._
import doobie.implicits._
import cats.effect.IO
import cats.implicits._

// Sketch of a program to run a query and get the output without knowing how many columns will
// come back, or their types. This can be useful for building query tools, etc.
object Dynamic {

  type Headers = List[String]
  type Data    = List[List[Object]]

  // Entry point. Run a query and print the results out.
  def main(args: Array[String]): Unit = {
    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:world",
      "postgres", ""
    )
    val (headers, data) = connProg("U%").transact(xa).unsafeRunSync()
    println(headers)
    data.foreach(println)
  }

  // Construct a parameterized query and process it with a custom program.
  def connProg(pattern: String): ConnectionIO[(Headers, Data)] = {
    val sql = "select code, name, population from country where code like ?"
    HC.prepareStatement(sql)(prepareAndExec(pattern))
  }

  // Configure and run a PreparedStatement. We don't know the column count or types.
  def prepareAndExec(pattern: String): PreparedStatementIO[(Headers, Data)] =
    for {
      _    <- HPS.set(pattern)
      md   <- HPS.getMetaData // lots of useful info here
      cols  = (1 to md.getColumnCount).toList
      data <- HPS.executeQuery(readAll(cols))
    } yield (cols.map(md.getColumnName), data)

  // Read the specified columns from the resultset.
  def readAll(cols: List[Int]): ResultSetIO[Data] =
    readOne(cols).whileM[List](HRS.next)

  // Take a list of column offsets and read a parallel list of values.
  def readOne(cols: List[Int]): ResultSetIO[List[Object]] =
    cols.traverse(FRS.getObject) // always works

}