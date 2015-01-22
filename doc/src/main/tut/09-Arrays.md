---
layout: book
number: 9
title: SQL Arrays
---

> SQL arrays are part of the JDBC specification but their behavior is vendor-specific.

### Setting Up

Note that the code in this chapter requires the `doobie-contrib-postgres` module.

Again we set up a transactor and pull in YOLO mode.

```tut:silent
import doobie.imports._
import scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
import xa.yolo._
```

This time we need an extra import to get PostgreSQL-specific type mappings.

```tut:silent
import doobie.contrib.postgresql.pgtypes._
```

Let's create a new table, which we will use for the examples to follow. Note that one of our columns is an array of `VARCHAR`.

```tut
val drop = sql"DROP TABLE IF EXISTS person".update.quick

val create = 
  sql"""
    CREATE TABLE person (
      id   SERIAL,
      name VARCHAR NOT NULL UNIQUE,
      age  SMALLINT,
      pets VARCHAR[] NOT NULL
    )
  """.update.quick

(drop *> create).run
```

### Reading and Writing

**doobie** maps SQL array columns to `Array`, `List`, and `Vector` by default.

```tut:silent
case class Person(id: Long, name: String, age: Option[Int], pets: List[String])

def insert(name: String, age: Option[Int], pets: List[String]): ConnectionIO[Person] = {
  sql"insert into person (name, age, pets) values ($name, $age, $pets)"
    .update
    .withUniqueGeneratedKeys[Person]("id", "name", "age", "pets")
}
```

```tut
insert("Bob", Some(12), List("Nixon", "Slappy")).quick.run
insert("Alice", None, Nil).quick.run
```


### Diving Deep

We can add a mapping from array types to `scalaz.IList` by invariant mapping.

```tut:silent
import scala.reflect.runtime.universe.TypeTag

implicit def IListAtom[A: TypeTag](implicit ev: Meta[List[A]]): Meta[IList[A]] =
  ev.xmap[IList[A]](IList.fromList, _.toList)
```

```tut
sql"select pets from person".query[IList[String]].quick.run
```

