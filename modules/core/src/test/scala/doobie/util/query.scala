// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{ ContextShift, IO, Timer }
import doobie._, doobie.implicits._
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext
import scala.Predef._
import scala.concurrent.duration._
import cats.syntax.all._
import cats.effect._
import cats.effect.concurrent.MVar


class queryspec extends Specification {

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)
  
  implicit def timer: Timer[IO] = 
    IO.timer(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1;LOCK_MODE=3",
    "sa", ""
  )

  val q = Query[String,Int]("select 123 where ? = 'foo'", None)
  val pairQuery = Query[String, (String, Int)]("select 'xxx', 123 where ? = 'foo'", None)

  "Query (non-empty)" >> {
    "to" in {
      q.to[List]("foo").transact(xa).unsafeRunSync() must_=== List(123)
    }
    "toMap" in {
      pairQuery.toMap[String, Int]("foo").transact(xa).unsafeRunSync() must_=== Map("xxx" -> 123)
    }
    "unique" in {
      q.unique("foo").transact(xa).unsafeRunSync() must_=== 123
    }
    "option" in {
      q.option("foo").transact(xa).unsafeRunSync() must_=== Some(123)
    }
    "map" in {
      q.map("x" * _).to[List]("foo").transact(xa).unsafeRunSync() must_=== List("x" * 123)
    }
    "contramap" in {
      q.contramap[Int](n => "foo" * n).to[List](1).transact(xa).unsafeRunSync() must_=== List(123)
    }
  }

  "Query (empty)" >> {
    "to" in {
      q.to[List]("bar").transact(xa).unsafeRunSync() must_=== Nil
    }
    "toMap" in {
      pairQuery.toMap[String, Int]("bar").transact(xa).unsafeRunSync() must_=== Map.empty
    }
    "unique" in {
      q.unique("bar").transact(xa).attempt.unsafeRunSync() must_=== Left(invariant.UnexpectedEnd)
    }
    "option" in {
      q.option("bar").transact(xa).unsafeRunSync() must_=== None
    }
    "map" in {
      q.map("x" * _).to[List]("bar").transact(xa).unsafeRunSync() must_=== Nil
    }
    "contramap" in {
      q.contramap[Int](n => "bar" * n).to[List](1).transact(xa).unsafeRunSync() must_=== Nil
    }
  }

  "Query0 from Query (non-empty)" >> {
    "to" in {
      q.toQuery0("foo").to[List].transact(xa).unsafeRunSync() must_=== List(123)
    }
    "toMap" in {
      pairQuery.toQuery0("foo").toMap[String, Int].transact(xa).unsafeRunSync() must_=== Map("xxx" -> 123)
    }
    "unique" in {
      q.toQuery0("foo").unique.transact(xa).unsafeRunSync() must_=== 123
    }
    "option" in {
      q.toQuery0("foo").option.transact(xa).unsafeRunSync() must_=== Some(123)
    }
    "map" in {
      q.toQuery0("foo").map(_ * 2).to[List].transact(xa).unsafeRunSync() must_=== List(246)
    }
  }

  "Query0 from Query (empty)" >> {
    "to" in {
      q.toQuery0("bar").to[List].transact(xa).unsafeRunSync() must_=== Nil
    }
    "toMap" in {
      pairQuery.toQuery0("bar").toMap[String, Int].transact(xa).unsafeRunSync() must_=== Map.empty
    }
    "unique" in {
      q.toQuery0("bar").unique.transact(xa).attempt.unsafeRunSync() must_=== Left(invariant.UnexpectedEnd)
    }
    "option" in {
      q.toQuery0("bar").option.transact(xa).unsafeRunSync() must_=== None
    }
    "map" in {
      q.toQuery0("bar").map(_ * 2).to[List].transact(xa).unsafeRunSync() must_=== Nil
    }
  }

  val q0n = Query0[Int]("select 123 where 'foo' = 'foo'", None)
  val pairQ0n = Query0[(String, Int)]("select 'xxx', 123 where 'foo' = 'foo'", None)

  "Query0 via constructor (non-empty)" >> {
    "to" in {
      q0n.to[List].transact(xa).unsafeRunSync() must_=== List(123)
    }
    "toMap" in {
      pairQ0n.toMap[String, Int].transact(xa).unsafeRunSync() must_=== Map("xxx" -> 123)
    }
    "unique" in {
      q0n.unique.transact(xa).unsafeRunSync() must_=== 123
    }
    "option" in {
      q0n.option.transact(xa).unsafeRunSync() must_=== Some(123)
    }
    "map" in {
      q0n.map(_ * 2).to[List].transact(xa).unsafeRunSync() must_=== List(246)
    }
  }

  val q0e = Query0[Int]("select 123 where 'bar' = 'foo'", None)
  val pairQ0e = Query0[(String, Int)]("select 'xxx', 123 where 'bar' = 'foo'", None)

  "Query0 via constructor (empty)" >> {
    "to" in {
      q0e.to[List].transact(xa).unsafeRunSync() must_=== Nil
    }
    "toMap" in {
      pairQ0e.toMap[String, Int].transact(xa).unsafeRunSync() must_=== Map.empty
    }
    "unique" in {
      q0e.unique.transact(xa).attempt.unsafeRunSync() must_=== Left(invariant.UnexpectedEnd)
    }
    "option" in {
      q0e.option.transact(xa).unsafeRunSync() must_=== None
    }
    "map" in {
      q0e.map(_ * 2).to[List].transact(xa).unsafeRunSync() must_=== Nil
    }
  }

  val qf = sql"select 'foo', ${1:Int}, ${Option.empty[Int]}, ${Option(42)}".query[String] // wrong!
  "Query to Fragment and back" >> {
    "test" in {
      val qfʹ = qf.toFragment.query[(String, Int, Option[Int], Option[Int])]
      qfʹ.unique.transact(xa).unsafeRunSync() must_=== (("foo", 1, None, Some(42)))
    }
  }

  val switchToSerialized = 
    sql"""SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL SERIALIZABLE"""

  val createQuery = sql"""create table foo (bar varchar primary key)"""
  val insertQuery = sql"""insert into foo values ('baz')"""

  "Locked query" >> {
    "cancel" in {
      val prg = (
        for {
          _               <- createQuery.update.run.transact(xa)
          isWaitedInLock  <- MVar.empty[IO, Boolean]

          insFib1 <- (
                (
                  switchToSerialized.update.run *>
                  insertQuery.update.run <* 
                  Async[ConnectionIO].liftIO(IO.sleep(4.second)) <*
                  Async[ConnectionIO].liftIO(isWaitedInLock.tryPut(true))
                ).transact(xa)
              ).start

          insFib2  <- (
                IO.sleep(1.second) *>
                IO.race(
                  (
                    switchToSerialized.update.run *>
                    insertQuery.update.run
                  ).transact(xa),
                  IO.sleep(2.seconds)
                ) *> isWaitedInLock.tryPut(false)
              ).start

          _   <- insFib1.join
          _   <- insFib2.join
          res <- isWaitedInLock.read
        } yield res)

      prg.unsafeRunSync() must_=== false
    }
  }
}