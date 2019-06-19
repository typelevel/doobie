---
layout: docs
number: 11
title: SQL Arrays
---

## {{page.title}}

This chapter shows how we can map Scala sequence types to SQL `ARRAY` types, for vendors that support it. Note that although SQL array mappings are part of the JDBC specification,  their behavior is vendor-specific and requires an add-on library; the code in this chapter requires `doobie-postgres`.

### Setting Up

Again we set up a transactor and pull in YOLO mode. We also need an import to get PostgreSQL-specific type mappings.

```scala mdoc:silent
import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.data._
import cats.effect._
import cats.implicits._

// We need a ContextShift[IO] before we can construct a Transactor[IO]. The passed ExecutionContext
// is where nonblocking operations will be executed. For testing here we're using a synchronous EC.
implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

// A transactor that gets connections from java.sql.DriverManager and executes blocking operations
// on an our synchronous EC. See the chapter on connection handling for more info.
val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver",     // driver classname
  "jdbc:postgresql:world",     // connect URL (driver-specific)
  "postgres",                  // user
  "",                          // password
  Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
)

val y = xa.yolo
import y._
```

```scala mdoc:invisible
implicit val mdocColors: doobie.util.Colors = doobie.util.Colors.None
```

### Reading and Writing Arrays

Let's create a new table with an SQL array column. Note that this is likely to work only for PostgreSQL; the syntax for arrays differs significantly from vendor to vendor.

```scala mdoc:silent
val drop = sql"DROP TABLE IF EXISTS person".update.quick

val create =
  sql"""
    CREATE TABLE person (
      id   SERIAL,
      name VARCHAR   NOT NULL UNIQUE,
      pets VARCHAR[] NOT NULL
    )
  """.update.quick
```

```scala mdoc
(drop *> create).unsafeRunSync
```

**doobie** maps SQL array columns to `Array`, `List`, and `Vector` by default. No special handling is required, other than importing the vendor-specific array support above.

```scala mdoc:silent
case class Person(id: Long, name: String, pets: List[String])

def insert(name: String, pets: List[String]): ConnectionIO[Person] = {
  sql"insert into person (name, pets) values ($name, $pets)"
    .update
    .withUniqueGeneratedKeys("id", "name", "pets")
}
```

Insert works fine, as does reading the result. No surprises.

```scala mdoc
insert("Bob", List("Nixon", "Slappy")).quick.unsafeRunSync
insert("Alice", Nil).quick.unsafeRunSync
```

### Lamentations of `NULL`

**doobie** maps nullable columns via `Option`, so `null` is never observed in programs that use the high-level API, and the typechecking feature discussed earlier will find mismatches. So this means if you have a nullable SQL `varchar[]` then you will be chided grimly if you don't map it as `Option[List[String]]` (or some other supported sequence type).

However there is another axis of variation here: the *array cells* themselves may contain null values. And the query checker can't save you here because this is not reflected in the metadata provided by PostgreSQL or H2, and probably not by anyone.

So there are actually four ways to map an array, and you should carefully consider which is appropriate for your schema. In the first two cases reading a `NULL` cell would result in a `NullableCellRead` exception.

```scala mdoc
sql"select array['foo','bar','baz']".query[List[String]].quick.unsafeRunSync
sql"select array['foo','bar','baz']".query[Option[List[String]]].quick.unsafeRunSync
sql"select array['foo',NULL,'baz']".query[List[Option[String]]].quick.unsafeRunSync
sql"select array['foo',NULL,'baz']".query[Option[List[Option[String]]]].quick.unsafeRunSync
```
