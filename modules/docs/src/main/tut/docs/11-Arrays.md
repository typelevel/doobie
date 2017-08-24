---
layout: docs
number: 11
title: SQL Arrays
---

## {{page.title}}

This chapter shows how we can map Scala sequence types to SQL `ARRAY` types, for vendors that support it. Note that although SQL array mappings are part of the JDBC specification,  their behavior is vendor-specific and requires an add-on library; the code in this chapter requires `doobie-postgres`.

### Setting Up

Again we set up a transactor and pull in YOLO mode. We also need an import to get PostgreSQL-specific type mappings.

```tut:silent
import doobie._, doobie.implicits._
import doobie.postgres._, doobie.postgres.implicits._
import cats._, cats.data._, cats.effect.IO, cats.implicits._
val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
val y = xa.yolo; import y._
```

### Reading and Writing Arrays

Let's create a new table with an SQL array column. Note that this is likely to work only for PostgreSQL; the syntax for arrays differs significantly from vendor to vendor.

```tut:silent
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

```tut
(drop *> create).unsafeRunSync
```

**doobie** maps SQL array columns to `Array`, `List`, and `Vector` by default. No special handling is required, other than importing the vendor-specific array support above.

```tut:silent
case class Person(id: Long, name: String, pets: List[String])

def insert(name: String, pets: List[String]): ConnectionIO[Person] = {
  sql"insert into person (name, pets) values ($name, $pets)"
    .update.withUniqueGeneratedKeys("id", "name", "pets")
}
```

Insert works fine, as does reading the result. No surprises.

```tut
insert("Bob", List("Nixon", "Slappy")).quick.unsafeRunSync
insert("Alice", Nil).quick.unsafeRunSync
```

### Lamentations of `NULL`

**doobie** maps nullable columns via `Option`, so `null` is never observed in programs that use the high-level API, and the typechecking feature discussed earlier will find mismatches. So this means if you have a nullable SQL `varchar[]` then you will be chided grimly if you don't map it as `Option[List[String]]` (or some other supported sequence type).

However there is another axis of variation here: the *array cells* themselves may contain null values. And the query checker can't save you here because this is not reflected in the metadata provided by PostgreSQL or H2, and probably not by anyone.

So there are actually four ways to map an array, and you should carefully consider which is appropriate for your schema. In the first two cases reading a `NULL` cell would result in a `NullableCellRead` exception.

```tut
sql"select array['foo','bar','baz']".query[List[String]].quick.unsafeRunSync
sql"select array['foo','bar','baz']".query[Option[List[String]]].quick.unsafeRunSync
sql"select array['foo',NULL,'baz']".query[List[Option[String]]].quick.unsafeRunSync
sql"select array['foo',NULL,'baz']".query[Option[List[Option[String]]]].quick.unsafeRunSync
```
