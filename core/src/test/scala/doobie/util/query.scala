package doobie.util

import scalaz._
import scalaz.concurrent._
import Scalaz._
import doobie.imports._
import org.specs2.mutable.Specification
import Predef._

object queryspec extends Specification {

  val xa = DriverManagerTransactor[Task](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  val q = Query[String,Int]("select 123 where ? = 'foo'", None)

  "Query (non-empty)" >> {
    "process" in {
      q.process("foo").list.transact(xa).unsafePerformSync must_=== List(123)
    }
    "sink" in {
      var x = Array(0)
      def effect(n: Int): ConnectionIO[Unit] = HC.delay(x(0) = n)      
      q.process("foo").sink(effect).transact(xa).unsafePerformSync
      x(0) must_=== 123
    }
    "to" in {
      q.to[List]("foo").transact(xa).unsafePerformSync must_=== List(123)
    }
    "accumulate" in {
      q.accumulate[List]("foo").transact(xa).unsafePerformSync must_=== List(123)
    }
    "unique" in {
      q.unique("foo").transact(xa).unsafePerformSync must_=== 123
    }
    "option" in {
      q.option("foo").transact(xa).unsafePerformSync must_=== Some(123)
    }
    "map" in {
      q.map("x" * _).to[List]("foo").transact(xa).unsafePerformSync must_=== List("x" * 123)
    }
    "contramap" in {
      q.contramap[Int](n => "foo" * n).to[List](1).transact(xa).unsafePerformSync must_=== List(123)
    }
  }

  "Query (empty)" >> {
    "process" in {
      q.process("bar").list.transact(xa).unsafePerformSync must_=== Nil
    }
    "sink" in {
      var x = Array(0)
      def effect(n: Int): ConnectionIO[Unit] = HC.delay(x(0) = n)      
      q.process("bar").sink(effect).transact(xa).unsafePerformSync
      x(0) must_=== 0
    }
    "to" in {
      q.to[List]("bar").transact(xa).unsafePerformSync must_=== Nil
    }
    "accumulate" in {
      q.accumulate[List]("bar").transact(xa).unsafePerformSync must_=== Nil
    }
    "unique" in {
      q.unique("bar").transact(xa).unsafePerformSyncAttempt must_=== -\/(invariant.UnexpectedEnd)
    }
    "option" in {
      q.option("bar").transact(xa).unsafePerformSync must_=== None
    }
    "map" in {
      q.map("x" * _).to[List]("bar").transact(xa).unsafePerformSync must_=== Nil
    }
    "contramap" in {
      q.contramap[Int](n => "bar" * n).to[List](1).transact(xa).unsafePerformSync must_=== Nil
    }
  }

  "Query0 from Query (non-empty)" >> {
    "process" in {
      q.toQuery0("foo").process.list.transact(xa).unsafePerformSync must_=== List(123)
    }
    "sink" in {
      var x = Array(0)
      def effect(n: Int): ConnectionIO[Unit] = HC.delay(x(0) = n)      
      q.toQuery0("foo").process.sink(effect).transact(xa).unsafePerformSync
      x(0) must_=== 123
    }
    "to" in {
      q.toQuery0("foo").to[List].transact(xa).unsafePerformSync must_=== List(123)
    }
    "accumulate" in {
      q.toQuery0("foo").accumulate[List].transact(xa).unsafePerformSync must_=== List(123)
    }
    "unique" in {
      q.toQuery0("foo").unique.transact(xa).unsafePerformSync must_=== 123
    }
    "option" in {
      q.toQuery0("foo").option.transact(xa).unsafePerformSync must_=== Some(123)
    }
    "map" in {
      q.toQuery0("foo").map(_ * 2).list.transact(xa).unsafePerformSync must_=== List(246)
    }
  }

  "Query0 from Query (empty)" >> {
    "process" in {
      q.toQuery0("bar").process.list.transact(xa).unsafePerformSync must_=== Nil
    }
    "sink" in {
      var x = Array(0)
      def effect(n: Int): ConnectionIO[Unit] = HC.delay(x(0) = n)      
      q.toQuery0("bar").process.sink(effect).transact(xa).unsafePerformSync
      x(0) must_=== 0
    }
    "to" in {
      q.toQuery0("bar").to[List].transact(xa).unsafePerformSync must_=== Nil
    }
    "accumulate" in {
      q.toQuery0("bar").accumulate[List].transact(xa).unsafePerformSync must_=== Nil
    }
    "unique" in {
      q.toQuery0("bar").unique.transact(xa).unsafePerformSyncAttempt must_=== -\/(invariant.UnexpectedEnd)
    }
    "option" in {
      q.toQuery0("bar").option.transact(xa).unsafePerformSync must_=== None
    }
    "map" in {
      q.toQuery0("bar").map(_ * 2).list.transact(xa).unsafePerformSync must_=== Nil
    }
  }

}
