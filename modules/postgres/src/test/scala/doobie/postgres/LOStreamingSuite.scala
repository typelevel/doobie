// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import fs2.{Chunk, Stream, Pure}
import cats.effect._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import doobie._, doobie.implicits._
import scala.concurrent.ExecutionContext.global


class LOStreamingSuite extends munit.ScalaCheckSuite {

  implicit val ioContextShift: ContextShift[IO] =
    IO.contextShift(global)

  private val blocker = Blocker.liftExecutionContext(global)

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  def genFiniteStream[F[_], A: Arbitrary]: Gen[Stream[F, A]] =
    arbitrary[Vector[Vector[A]]].map { chunks =>
      chunks.map { chunk =>
        Stream.chunk(Chunk.seq(chunk))
      }.foldLeft(Stream.empty.covaryAll[F, A])(_ ++ _)
    }

  test("large object streaming should round-trip") {
    forAll(genFiniteStream[Pure, Byte]) { data =>
      val data0 = data.covary[ConnectionIO]

      val result = Stream.bracket(PHLOS.createLOFromStream(data0, blocker))(
        oid => PHC.pgGetLargeObjectAPI(PFLOM.unlink(oid))
      ).flatMap(oid => PHLOS.createStreamFromLO(oid, chunkSize = 1024 * 10, blocker))
        .compile.toVector.transact(xa).unsafeRunSync()

      assertEquals(result, data.toVector)
    }
  }


}
