// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.{ IO, Sync }
import doobie._, doobie.implicits._
import java.io.ByteArrayOutputStream


class PGCopySuite extends munit.FunSuite {

  import cats.effect.unsafe.implicits.global

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  test("copy out should read csv in utf-8 and match expectations") {

    val query = """
      copy (select code, name, population
            from country
            where name like 'U%'
            order by code)
      to stdout (encoding 'utf-8', format csv)"""

    val fixture = """
      |ARE,United Arab Emirates,2441000
      |GBR,United Kingdom,59623400
      |UGA,Uganda,21778000
      |UKR,Ukraine,50456000
      |UMI,United States Minor Outlying Islands,0
      |URY,Uruguay,3337000
      |USA,United States,278357000
      |UZB,Uzbekistan,24318000
      |
    """.trim.stripMargin

    val prog: ConnectionIO[String] =
      for {
        out <- Sync[ConnectionIO].delay(new ByteArrayOutputStream)
        _   <- PHC.pgGetCopyAPI(PFCM.copyOut(query, out))
      } yield new String(out.toByteArray, "UTF-8")

    assertEquals(prog.transact(xa).unsafeRunSync(), fixture)

  }

}
