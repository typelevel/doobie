---
layout: book
number: 5
title: Parameterized Queries
---

In this short chapter we learn how to construct parameterized queries.

### Setting Up

Same as last chapter, so if you're still set up you can skip this section. Otherwise let's set up an in-memory database.

```tut:silent
import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.h2.Driver",                      // driver class
  "jdbc:h2:mem:ch5;DB_CLOSE_DELAY=-1",  // connect URL
  "sa", ""                              // user and pass
)
```

And then load up our sample data and pull in YOLO mode to save some typing.

```tut
sql"RUNSCRIPT FROM 'world.sql' CHARSET 'UTF-8'".update.run.transact(xa).run
import xa.yolo._
```

This time we're still be playing with the `country` table, but just with a few columns, shown here.

```sql
CREATE TABLE country (
  code       character(3)  NOT NULL,
  name       text          NOT NULL,
  population integer       NOT NULL,
  gnp        numeric(10,2)
  -- more columns, but we won't use them here
)
```


### Parameterized Queries

Let's set up our Country class and re-run last chapter's query just to review.

```tut:silent
case class Country(code: String, name: String, pop: Int, gnp: Option[Double])
```

```tut
(sql"select code, name, population, gnp from country"
  .query[Country].process.take(5).quick.run)
```

Still works. Ok. 

So let's factor our query into a method and add a parameter that selects only the countries with a population larger than some value the user will provide. We insert the `minProp` argument into our SQL statement as `$minPop`, just as if we were doing string interpolation.

```tut:silent
def biggerThan(minPop: Int) = sql"""
  select code, name, population, gnp 
  from country
  where population > $minPop
""".query[Country]
```

And when we run the query ... surprise, it works!

```tut
biggerThan(150000000).quick.run // Let's see them all
```

So what's going on? It looks like we're just dropping a string literal into our SQL string, but actually wer'e constructing a proper parameterized `PreparedStatement`, and the `minProp` value is ultimately set via a call to `setInteger` (see "Diving Deeper" below.)

**doobie** allows you to interpolate any JVM type that has a target mapping defined by the JDBC spec, plus vendor-specific types and custom column types that you define. We will discuss custom type mappings in a later chapter.

### Diving Deeper

TODO: explain in terms of `hi` combinators.







