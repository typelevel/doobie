// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.munit

// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

import cats.effect.IO
import doobie.syntax.string.*
import doobie.util.Read
import doobie.util.transactor.Transactor
import munit.*

trait CheckerChecks[M[_]] extends FunSuite with Checker[M] {

  lazy val transactor: Transactor[M] = Transactor.fromDriverManager[M](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  test("trivial") { check(sql"select 1".query[Int]) }
  test("fail".fail) { check(sql"select 1".query[String]) }

  test("trivial case-class") {
    import doobie.generic.auto.*

    check(sql"select 1".query[CheckerChecks.Foo[cats.Id]])
  }

  test("Read should select correct columns when combined with `product`") {
    import cats.syntax.all.*
    import doobie.implicits.*

    val ri = Read[Int]
    val rs = Read[String]

    // tupled use product under the hood
    val combined: Read[(Int, String)] = (ri, rs).tupled

    check(sql"SELECT 1, '2'".query(using combined))
  }

  test("Read should select correct columns for checking when combined with `ap`") {
    val readInt = Read[(Int, Int)]
    val readIntToInt: Read[Tuple2[Int, Int] => String] =
      Read[(String, String)].map(i => k => s"$i,$k")

    val combined: Read[String] = readInt.ap(readIntToInt)

    check(sql"SELECT '1', '2', 3, 4".query(using combined))
  }

}

object CheckerChecks {
  final case class Foo[F[_]](x: Int)
}

class IOCheckerCheck extends CheckerChecks[IO] with IOChecker {}
