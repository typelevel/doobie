---
layout: book
number: 14
title: Extensions for H2
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

With an outer join you end up with set of nullable columns, which you typically want to map to a single `Option` of some composite type. The most straightforward way do this is by using the `map` method on `Query0` to transform the result type, using applicative composition to combine the optional values:

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
