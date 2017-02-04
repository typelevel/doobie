package doobie.util

#+scalaz
import scalaz._
import scalaz.concurrent._
import Scalaz._
#-scalaz
#+cats
import scala.util.{ Left => -\/, Right => \/- }
import fs2.interop.cats._
#-cats
import doobie.imports._
import org.specs2.mutable.Specification
import Predef._


object queryspec extends Specification {

  val xa = DriverManagerTransactor[IOLite](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  val q = Query[String,Int]("select 123 where ? = 'foo'", None)

  "Query (non-empty)" >> {
#+scalaz
    "process" in {
      q.process("foo").list.transact(xa).unsafePerformIO must_=== List(123)
    }
    "sink" in {
      var x = Array(0)
      def effect(n: Int): ConnectionIO[Unit] = HC.delay(x(0) = n)
      q.process("foo").sink(effect).transact(xa).unsafePerformIO
      x(0) must_=== 123
    }
    "accumulate" in {
      q.accumulate[List]("foo").transact(xa).unsafePerformIO must_=== List(123)
    }
#-scalaz
    "to" in {
      q.to[List]("foo").transact(xa).unsafePerformIO must_=== List(123)
    }
    "unique" in {
      q.unique("foo").transact(xa).unsafePerformIO must_=== 123
    }
    "option" in {
      q.option("foo").transact(xa).unsafePerformIO must_=== Some(123)
    }
    "map" in {
      q.map("x" * _).to[List]("foo").transact(xa).unsafePerformIO must_=== List("x" * 123)
    }
    "contramap" in {
      q.contramap[Int](n => "foo" * n).to[List](1).transact(xa).unsafePerformIO must_=== List(123)
    }
  }

  "Query (empty)" >> {
#+scalaz
    "process" in {
      q.process("bar").list.transact(xa).unsafePerformIO must_=== Nil
    }
    "sink" in {
      var x = Array(0)
      def effect(n: Int): ConnectionIO[Unit] = HC.delay(x(0) = n)
      q.process("bar").sink(effect).transact(xa).unsafePerformIO
      x(0) must_=== 0
    }
    "accumulate" in {
      q.accumulate[List]("bar").transact(xa).unsafePerformIO must_=== Nil
    }
#-scalaz
    "to" in {
      q.to[List]("bar").transact(xa).unsafePerformIO must_=== Nil
    }
    "unique" in {
      q.unique("bar").transact(xa).attempt.unsafePerformIO must_=== -\/(invariant.UnexpectedEnd)
    }
    "option" in {
      q.option("bar").transact(xa).unsafePerformIO must_=== None
    }
    "map" in {
      q.map("x" * _).to[List]("bar").transact(xa).unsafePerformIO must_=== Nil
    }
    "contramap" in {
      q.contramap[Int](n => "bar" * n).to[List](1).transact(xa).unsafePerformIO must_=== Nil
    }
  }

  "Query0 from Query (non-empty)" >> {
#+scalaz
    "process" in {
      q.toQuery0("foo").process.list.transact(xa).unsafePerformIO must_=== List(123)
    }
    "sink" in {
      var x = Array(0)
      def effect(n: Int): ConnectionIO[Unit] = HC.delay(x(0) = n)
      q.toQuery0("foo").process.sink(effect).transact(xa).unsafePerformIO
      x(0) must_=== 123
    }
    "accumulate" in {
      q.toQuery0("foo").accumulate[List].transact(xa).unsafePerformIO must_=== List(123)
    }
#-scalaz
    "to" in {
      q.toQuery0("foo").to[List].transact(xa).unsafePerformIO must_=== List(123)
    }
    "unique" in {
      q.toQuery0("foo").unique.transact(xa).unsafePerformIO must_=== 123
    }
    "option" in {
      q.toQuery0("foo").option.transact(xa).unsafePerformIO must_=== Some(123)
    }
    "map" in {
      q.toQuery0("foo").map(_ * 2).list.transact(xa).unsafePerformIO must_=== List(246)
    }
  }

  "Query0 from Query (empty)" >> {
#+scalaz
    "process" in {
      q.toQuery0("bar").process.list.transact(xa).unsafePerformIO must_=== Nil
    }
    "sink" in {
      var x = Array(0)
      def effect(n: Int): ConnectionIO[Unit] = HC.delay(x(0) = n)
      q.toQuery0("bar").process.sink(effect).transact(xa).unsafePerformIO
      x(0) must_=== 0
    }
    "accumulate" in {
      q.toQuery0("bar").accumulate[List].transact(xa).unsafePerformIO must_=== Nil
    }
#-scalaz
    "to" in {
      q.toQuery0("bar").to[List].transact(xa).unsafePerformIO must_=== Nil
    }
    "unique" in {
      q.toQuery0("bar").unique.transact(xa).attempt.unsafePerformIO must_=== -\/(invariant.UnexpectedEnd)
    }
    "option" in {
      q.toQuery0("bar").option.transact(xa).unsafePerformIO must_=== None
    }
    "map" in {
      q.toQuery0("bar").map(_ * 2).list.transact(xa).unsafePerformIO must_=== Nil
    }
  }

  val q0n = Query0[Int]("select 123 where 'foo' = 'foo'", None)

  "Query0 via constructor (non-empty)" >> {
#+scalaz
    "process" in {
      q0n.process.list.transact(xa).unsafePerformIO must_=== List(123)
    }
    "sink" in {
      var x = Array(0)
      def effect(n: Int): ConnectionIO[Unit] = HC.delay(x(0) = n)
      q0n.process.sink(effect).transact(xa).unsafePerformIO
      x(0) must_=== 123
    }
    "accumulate" in {
      q0n.accumulate[List].transact(xa).unsafePerformIO must_=== List(123)
    }
#-scalaz
    "to" in {
      q0n.to[List].transact(xa).unsafePerformIO must_=== List(123)
    }
    "unique" in {
      q0n.unique.transact(xa).unsafePerformIO must_=== 123
    }
    "option" in {
      q0n.option.transact(xa).unsafePerformIO must_=== Some(123)
    }
    "map" in {
      q0n.map(_ * 2).list.transact(xa).unsafePerformIO must_=== List(246)
    }
  }

  val q0e = Query0[Int]("select 123 where 'bar' = 'foo'", None)

  "Query0 via constructor (empty)" >> {
#+scalaz
    "process" in {
      q0e.process.list.transact(xa).unsafePerformIO must_=== Nil
    }
    "sink" in {
      var x = Array(0)
      def effect(n: Int): ConnectionIO[Unit] = HC.delay(x(0) = n)
      q0e.process.sink(effect).transact(xa).unsafePerformIO
      x(0) must_=== 0
    }
    "accumulate" in {
      q0e.accumulate[List].transact(xa).unsafePerformIO must_=== Nil
    }
#-scalaz
    "to" in {
      q0e.to[List].transact(xa).unsafePerformIO must_=== Nil
    }
    "unique" in {
      q0e.unique.transact(xa).attempt.unsafePerformIO must_=== -\/(invariant.UnexpectedEnd)
    }
    "option" in {
      q0e.option.transact(xa).unsafePerformIO must_=== None
    }
    "map" in {
      q0e.map(_ * 2).list.transact(xa).unsafePerformIO must_=== Nil
    }
  }

}
