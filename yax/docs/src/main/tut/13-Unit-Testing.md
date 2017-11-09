---
layout: book
number: 13
title: Unit Testing
---

The YOLO-mode query checking feature demonstated in an earlier chapter is also available as a trait you can mix into your [Specs2](http://etorreborre.github.io/specs2/) or [ScalaTest](http://www.scalatest.org/) unit tests.

### Setting Up

#+scalaz
As with earlier chapters we set up a `Transactor` and YOLO mode. We will also use the `doobie-specs2` and `doobie-scalatest` add-ons.
#-scalaz
#+cats
As with earlier chapters we set up a `Transactor` and YOLO mode. We will also use the `doobie-specs2-cats` and `doobie-scalatest-cats` add-ons.
#-cats
If you run into any difficulties it may be useful to refer to the [Working Around Bad Metadata Section](06-Checking.html)

```tut:silent
import doobie.imports._
#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.data._, cats.implicits._
import fs2.interop.cats._
#-cats
val xa = DriverManagerTransactor[IOLite](
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

#+scalaz
The `doobie-specs2` add-on provides a mix-in trait that we can add to a `Specification` to allow for typechecking of queries, interpreted as a set of specifications.
#-scalaz
#+cats
The `doobie-specs2-cats` add-on provides a mix-in trait that we can add to a `Specification` to allow for typechecking of queries, interpreted as a set of specifications.
#-cats

Our unit test needs to extend `AnalysisSpec` and must define a `Transactor[IOLite]`. To construct a testcase for a query, pass it to the `check` method. Note that query arguments are never used, so they can be any values that typecheck.

```tut:silent
import doobie.util.iolite.IOLite
import doobie.specs2.imports._
import org.specs2.mutable.Specification

object AnalysisTestSpec extends Specification with AnalysisSpec {

  val transactor = DriverManagerTransactor[IOLite](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )

  check(trivial)
  check(biggerThan(0))
  check(update("", ""))

}
```

When we run the test we get output similar to what we saw in the previous chapter on checking queries, but each item is now a test. Note that doing this in the REPL is a little awkward; in real source you would get the source file and line number associated with each query.

```tut:plain
{ specs2 run AnalysisTestSpec; () } // pretend this is sbt> test
```

### The ScalaTest Package

#+scalaz
The `doobie-scalatest` add-on provides a mix-in trait that we can add to any `Assertions` implementation (like `FunSuite`) much like the Specs2 package above.
#-scalaz
#+cats
The `doobie-scalatest-cats` add-on provides a mix-in trait that we can add to any `Assertions` implementation (like `FunSuite`) much like the Specs2 package above.
#-cats

```tut:silent
import doobie.scalatest.imports._
import org.scalatest._

class AnalysisTestScalaCheck extends FunSuite with Matchers with IOLiteChecker {

  val transactor = DriverManagerTransactor[IOLite](
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
