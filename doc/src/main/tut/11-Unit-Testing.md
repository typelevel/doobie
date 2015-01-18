---
layout: book
number: 11
title: Unit Testing
---

### Setting Up

```tut:silent
import doobie.imports._
import scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.h2.Driver",                      
  "jdbc:h2:mem:ch11;DB_CLOSE_DELAY=-1", 
  "sa", ""                              
)
sql"RUNSCRIPT FROM 'world.sql' CHARSET 'UTF-8'".update.run.transact(xa).run
```

And again, playing with the `country` table, here again for reference.

```sql
CREATE TABLE country (
  code        character(3)  NOT NULL,
  name        text          NOT NULL,
  population  integer NOT NULL,
  gnp         numeric(10,2),
  indepyear   smallint
  -- more columns, but we won't use them here
)
```

### The Specs Package

Some queries.

```tut:silent
case class Country(code: Int, name: String, pop: Int, gnp: Double)

val trivial = sql"""
  select 42, 'foo'
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

A unit test, which just needs to mention the queries. The arguments are never used so they can be any values that typecheck. We must define a transactor to be used by the checker.

```tut:silent
import doobie.contrib.specs2.AnalysisSpec
import org.specs2.mutable.Specification

object AnalysisTestSpec extends Specification with AnalysisSpec {

  val transactor = DriverManagerTransactor[Task](
    "org.h2.Driver",                      
    "jdbc:h2:mem:ch11;DB_CLOSE_DELAY=-1", 
    "sa", ""                              
  )

  check(trivial)
  check(biggerThan(0))
  check(update("", ""))

}
```

When we run the test we get output similar to what we saw in the previous chapter on checking queries, but each item is now a test. Note that doing this in the REPL is a little awkward; in real source you would get the source file and line number associated with each query.

```tut:plain
specs2 run AnalysisTestSpec
```



