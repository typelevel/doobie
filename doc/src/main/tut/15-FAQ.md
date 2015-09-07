---
layout: book
number: 15
title: Frequently-Asked Questions
---

In this chapter we address some frequently-asked questions. Setup is the same as most prior chapters.

```tut:silent
import doobie.imports._
import scalaz._, Scalaz._
import scalaz.concurrent.Task

val xa = DriverManagerTransactor[Task](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)

import xa.yolo._
```

### How do I handle outer joins?

With an outer join you end up with set of nullable columns, which you typically want to map to a single `Option` of some composite type. The most straightforward way do this is to select the `Option` columns directly, then use the `map` method on `Query0` to transform the result type using applicative composition on the optional values:

```tut:silent
case class Country(name: String, code: String)
case class City(name: String, district: String)

val join: Query0[(Country, Option[City])] = 
  sql"""
    select c.name, c.code,
           k.name, k.district 
    from country c 
    left outer join city k 
    on c.capital = k.id
  """.query[(Country, Option[String], Option[String])].map {
    case (c, n, d) => (c, (n |@| d)(City))
  }
```

Some examples, filtered for size.

```tut
join.process.filter(_._1.name.startsWith("United")).quick.run
```

### How do I do an `IN` clause?

The `sql` interpolator cannot currently expand a sequence parameter `IN ($x)` to `IN (?, ?, ...)`, but we can define some machinery that gets the job done.

TODO: IN and non-IN params

```tut:silent
def placeholders(n: Int): String =
  List.fill(n)("?").mkString(",")

// SQL string with explicit placeholders
def mkSql(codes: List[String]): String =
  s"""
    SELECT name, code, population 
    FROM country 
    WHERE code IN (${placeholders(codes)})
  """

// TODO: abstract over List and String
def mkSet(codes: List[String]): PreparedStatementIO[Unit] =
  codes.zipWithIndex.foldRight(().point[PreparedStatementIO]) { case ((s, i), p) =>
    HPS.set(i + 1, s) *> p
  }

val codes = List("USA", "FRA")

val proc = HC.process[(String, String, BigDecimal)](mkSql(codes), mkSet(codes))
```

```tut
proc.quick.run
```


### How do I do several things in the same transaction?

