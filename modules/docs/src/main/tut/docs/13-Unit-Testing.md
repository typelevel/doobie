---
layout: docs
number: 13
title: Unit Testing
---

## {{page.title}}

The YOLO-mode query checking feature demonstated in an earlier chapter is also available as a trait you can mix into your [Specs2](http://etorreborre.github.io/specs2/) or [ScalaTest](http://www.scalatest.org/) unit tests.

### Setting Up

As with earlier chapters we set up a `Transactor` and YOLO mode. We will also use the `doobie-specs2` and `doobie-scalatest` add-ons.

```scala mdoc:silent
import doobie._
import doobie.implicits._
import cats._
import cats.data._
import cats.effect._
import cats.implicits._

// We need a ContextShift[IO] before we can construct a Transactor[IO]. The passed ExecutionContext
// is where nonblocking operations will be executed. For testing here we're using a synchronous EC.
implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

// A transactor that gets connections from java.sql.DriverManager and executes blocking operations
// on an our synchronous EC. See the chapter on connection handling for more info.
val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver",     // driver classname
  "jdbc:postgresql:world",     // connect URL (driver-specific)
  "postgres",                  // user
  "",                          // password
  Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
)
```

```scala mdoc:invisible
implicit val mdocColors: doobie.util.Colors = doobie.util.Colors.None
```

And again we are playing with the `country` table, given here for reference.

```sql
CREATE TABLE country (
  code        character(3)  NOT NULL,
  name        text          NOT NULL,
  population  integer       NOT NULL,
  gnp         numeric(10,2),
  indepyear   smallint
  -- more columns, but we won't use them here
)
```

So here are a few queries we would like to check. Note that we can only check values of type `Query0` and `Update0`; we can't check `Process` or `ConnectionIO` values, so a good practice is to define your queries in a DAO module and apply further operations at a higher level.

```scala mdoc:silent
case class Country(code: Int, name: String, pop: Int, gnp: Double)

val trivial =
  sql"""
    select 42, 'foo'::varchar
  """.query[(Int, String)]

def biggerThan(minPop: Short) =
  sql"""
    select code, name, population, gnp, indepyear
    from country
    where population > $minPop
  """.query[Country]

def update(oldName: String, newName: String) =
  sql"""
    update country set name = $newName where name = $oldName
  """.update
```

### The Specs2 Package

The `doobie-specs2` add-on provides a mix-in trait that we can add to a `Specification` to allow for typechecking of queries, interpreted as a set of specifications.

Our unit test needs to extend `AnalysisSpec` and must define a `Transactor[IO]`. To construct a testcase for a query, pass it to the `check` method. Note that query arguments are never used, so they can be any values that typecheck.

```scala mdoc:silent
import org.specs2.mutable.Specification

object AnalysisTestSpec extends Specification with doobie.specs2.IOChecker {

  val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )

  check(trivial)
  check(biggerThan(0))
  check(update("", ""))

}
```

When we run the test we get output similar to what we saw in the previous chapter on checking queries, but each item is now a test. Note that doing this in the REPL is a little awkward; in real source you would get the source file and line number associated with each query.

```scala mdoc
import _root_.specs2.{ run => runTest }
import _root_.org.specs2.main.{ Arguments, Report }

// Run a test programmatically. Usually you would do this from sbt, bloop, etc.
runTest(AnalysisTestSpec)(Arguments(report = Report(_color = Some(false))))
```

### The ScalaTest Package

The `doobie-scalatest` add-on provides a mix-in trait that we can add to any `Assertions` implementation (like `FunSuite`) much like the Specs2 package above.

```scala mdoc:silent
import org.scalatest._

class AnalysisTestScalaCheck extends FunSuite with Matchers with doobie.scalatest.IOChecker {

  override val colors = doobie.util.Colors.None // just for docs

  val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )

  test("trivial")    { check(trivial)        }
  test("biggerThan") { check(biggerThan(0))  }
  test("update")     { check(update("", "")) }

}
```

Details are shown for failing tests.

```scala mdoc
// Run a test programmatically. Usually you would do this from sbt, bloop, etc.
(new AnalysisTestScalaCheck).execute(color = false)
```
