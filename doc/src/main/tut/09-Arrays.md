---
layout: book
number: 9
title: SQL Arrays
---

> SQL arrays are part of the JDBC specification but their behavior is vendor-specific.

### Setting Up

Again we set up an H2 transactor and pull in YOLO mode, but this time we're not using the world database.

```tut:silent
import doobie.imports._
import scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.h2.Driver",                      
  "jdbc:h2:mem:ch9;DB_CLOSE_DELAY=-1",
  "sa", ""                              
)
import xa.yolo._
```

This time we need an extra import to get the H2-specific type mappings.

```tut:silent
import doobie.contrib.h2.h2types._
```

Let's create a new table, which we will use for the examples to follow. Note that one of our columns has a SQL `ARRAY` type. In H2 the array type is essentially unconstrained, so it takes a degree of trust to use it. Other databases like PostgreSQL have arrays whose elements are constrained to a specific type.

```tut
sql"""
  CREATE TABLE person (
    id   BIGINT IDENTITY,
    name VARCHAR(255) NOT NULL UNIQUE,
    age  TINYINT,
    pets ARRAY NOT NULL
  )""".update.quick.run
```

### Reading and Writing

**doobie** maps SQL `ARRAY` columns to `Array`, `List`, and `Vector` by default, but you can easily add a mapping to any sequence type you like.

```tut:silent
case class Person(id: Long, name: String, age: Option[Int], pets: List[String])

def insert(name: String, age: Option[Int], pets: List[String]): ConnectionIO[Person] =
  for {
    _  <- sql"insert into person (name, age, pets) values ($name, $age, $pets)".update.run
    id <- sql"call identity()".query[Long].unique
    p  <- sql"select id, name, age, pets from person where id = $id".query[Person].unique
  } yield p
```

```tut
insert("Bob", Some(12), List("Nixon", "Slappy")).quick.run
insert("Alice", None, Nil).quick.run
```


### Diving Deep

```tut
import scala.reflect.runtime.universe.TypeTag

implicit def IListAtom[A: TypeTag](implicit ev: Meta[List[A]]): Meta[IList[A]] =
  ev.xmap[IList[A]](IList.fromList, _.toList)
```

```tut:silent
sql"select pets from person".query[IList[String]].quick.run
```

