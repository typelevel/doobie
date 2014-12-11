---
layout: book
number: 5
title: Parameterized Queries
---

### Setting Up

Same as last chapter, so if you're still set up you can skip this section. Otherwise let's set up an in-memory database.

```tut:silent
import doobie.imports._
import scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.h2.Driver",                      // driver class
  "jdbc:h2:mem:ch5;DB_CLOSE_DELAY=-1", // connect URL
  "sa", ""                              // user and pass
)
```

And then load up our sample data.

```tut
sql"RUNSCRIPT FROM 'world.sql' CHARSET 'UTF-8'".update.run.transact(xa).run
```

Let's also pull in YOLO mode to save some typing.

```tut:silent
import xa.yolo._
```

This time we'll still be playing with the `country` table, but just with a few columns, shown here.

```sql
CREATE TABLE country (
  code character(3)  NOT NULL,
  name text          NOT NULL,
  population integer NOT NULL,
  gnp numeric(10,2)
  -- more columns, but we won't use them here
)
```


### Parameterized Queries

Let's set up our Country class again.

```tut:silent
case class Country(code: String, name: String, pop: Int, gnp: Option[Double])
```

And try our query again, just to be sure.

```tut
sql"""
  select code, name, population, gnp 
  from country
""".query[Country].process.take(5).quick.run
```

Still works. Ok. So let's add a parameter. 

```tut
def biggerThan(minPop: Int) = sql"""
  select code, name, population, gnp 
  from country
  where population > $minPop
""".query[Country]
biggerThan(150000000).quick.run // Let's see them all
```

So what's going on? It looks like we're just dropping an `Int`  into our SQL statement, but actually this becomes a proper parameterized `PreparedStatement`, and the value is ultimately set via a call to `setInteger`. 

**doobie** allows you to interpolate any JVM type that has a target mapping defined by the JDBC spec, plus vendor-specific types and custom column types that you define. We will get to this customization in a later chapter.









