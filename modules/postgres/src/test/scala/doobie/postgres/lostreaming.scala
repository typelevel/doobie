// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import fs2.{Chunk, Stream, Pure}
import cats.effect.{ ContextShift, IO }
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import cats.effect._
import cats.implicits._
import doobie._, doobie.implicits._
import doobie.postgres._, doobie.postgres.implicits._
import java.util.concurrent.{ Executors, ThreadFactory }
import org.specs2.mutable.Specification
import org.specs2.ScalaCheck
import scala.concurrent.ExecutionContext

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object lostreamingspec extends Specification with ScalaCheck {
  import HC.ContextShift.Implicits.global

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  val blockingContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool(
      new ThreadFactory {
        def newThread(r: Runnable): Thread = {
          val th = new Thread(r)
          th.setName(s"lostreamingspec-blocking-${th.getId}")
          th.setDaemon(true)
          th
        }
      }
    ))

  def genFiniteStream[F[_], A: Arbitrary]: Gen[Stream[F, A]] =
    arbitrary[Vector[Vector[A]]].map { chunks =>
      chunks.map { chunk =>
        Stream.chunk(Chunk.seq(chunk))
      }.foldLeft(Stream.empty.covaryAll[F, A])(_ ++ _)
    }

  "large object streaming" should {
    "round-trip" in forAll(genFiniteStream[Pure, Byte]) { data =>
      val data0 = data.covary[ConnectionIO]

      val result = Stream.bracket(PHLOS.createLOFromStream(data0, blockingContext))(
        oid => PHC.pgGetLargeObjectAPI(PFLOM.unlink(oid))
      ).flatMap(oid => PHLOS.createStreamFromLO(oid, chunkSize = 1024 * 10, blockingContext))
       .compile.toVector.transact(xa).unsafeRunSync()

      result must_=== data.toVector
    }
  }
}
