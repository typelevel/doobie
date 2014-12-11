---
layout: book
number: 6
title: Checking Queries
---

### Setting Up

Same as last chapter, so if you're still set up you can skip this section. 

```tut:silent
import doobie.imports._
import scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.h2.Driver",                      // driver class
  "jdbc:h2:mem:ch6;DB_CLOSE_DELAY=-1",  // connect URL
  "sa", ""                              // user and pass
)
```

Sample data and YOLO mode.

```tut
sql"RUNSCRIPT FROM 'world.sql' CHARSET 'UTF-8'".update.run.transact(xa).run
import xa.yolo._
```

And again, playing with the `country` table, here again for reference.

```sql
CREATE TABLE country (
  code character(3)  NOT NULL,
  name text          NOT NULL,
  population integer NOT NULL,
  gnp numeric(10,2)
  -- more columns, but we won't use them here
)
```

### Checking a Query

Let's redefine our `Country` class, just for fun.

```tut:silent
case class Country(code: Int, name: String, pop: Int, gnp: Double)
```

Ok here's our parameterized query from last chapter, but with the new `Country` definition and `minPop` as a `Short`. Looks the same but means something different because the mapped types have changed.

```tut
def biggerThan(minPop: Short) = sql"""
  select code, name, population, gnp 
  from country
  where population > $minPop
""".query[Country]
```

So let's try the `check` method provided by YOLO and see what happens.

```tut
biggerThan(0).check.run
```

