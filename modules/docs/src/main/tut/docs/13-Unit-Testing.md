---
layout: docs
number: 13
title: Unit Testing
---

## {{page.title}}

The YOLO-mode query checking feature demonstated in an earlier chapter is also available as a trait you can mix into your [Specs2](http://etorreborre.github.io/specs2/) or [ScalaTest](http://www.scalatest.org/) unit tests.

### Setting Up

As with earlier chapters we set up a `Transactor` and YOLO mode. We will also use the `doobie-specs2` and `doobie-scalatest` add-ons.

```tut:silent
import doobie._, doobie.implicits._
import cats._, cats.data._, cats.effect.IO, cats.implicits._
val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
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

```tut:silent
case class Country(code: Int, name: String, pop: Int, gnp: Double)

val trivial = sql"""
  select 42, 'foo'::varchar
""".query[(Int, String)]

def biggerThan(minPop: Short) = sql"""
  select code, name, population, gnp, indepyear
  from country
  where population > $minPop
""".query[Country]

def update(oldName: String, newName: String) = sql"""
  update country set name = $newName where name = $oldName
""".update
```

### The Specs2 Package

The `doobie-specs2` add-on provides a mix-in trait that we can add to a `Specification` to allow for typechecking of queries, interpreted as a set of specifications.

Our unit test needs to extend `AnalysisSpec` and must define a `Transactor[IO]`. To construct a testcase for a query, pass it to the `check` method. Note that query arguments are never used, so they can be any values that typecheck.

```tut:silent
import doobie.specs2._
import org.specs2.mutable.Specification

object AnalysisTestSpec extends Specification with IOChecker {

  val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )

  check(trivial)
  check(biggerThan(0))
  check(update("", ""))

}
```

When we run the test we get output similar to what we saw in the previous chapter on checking queries, but each item is now a test. Note that doing this in the REPL is a little awkward; in real source you would get the source file and line number associated with each query.

```tut:plain
{ _root_.specs2.run(AnalysisTestSpec); () } // pretend this is sbt> test
```

### The ScalaTest Package

The `doobie-scalatest` add-on provides a mix-in trait that we can add to any `Assertions` implementation (like `FunSuite`) much like the Specs2 package above.

```tut:silent
import doobie.scalatest.imports._
import org.scalatest._

class AnalysisTestScalaCheck extends FunSuite with Matchers with IOChecker {

  val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )

  test("trivial")    { check(trivial)        }
  test("biggerThan") { check(biggerThan(0))  }
  test("update")     { check(update("", "")) }

}
```

Details are shown for failing tests.

```tut:plain
(new AnalysisTestScalaCheck).execute() // pretend this is sbt> test
```
