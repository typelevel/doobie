---
layout: book
number: 11
title: SQL Arrays
---

This chapter shows how we can map Scala sequence types to SQL `ARRAY` types, for vendors that support it. Note that although SQL array mappings are part of the JDBC specification,  their behavior is vendor-specific and requires an add-on library; the code in this chapter requires `doobie-contrib-postgresql`.

### Setting Up

Again we set up a transactor and pull in YOLO mode. We also need an import to get PostgreSQL-specific type mappings.

```tut:silent
import doobie.imports._
import doobie.postgres.imports._
#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.data._, cats.implicits._
import fs2.interop.cats._
#-cats
val xa = DriverManagerTransactor[IOLite](
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
(drop *> create).unsafePerformIO
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
insert("Bob", List("Nixon", "Slappy")).quick.unsafePerformIO
insert("Alice", Nil).quick.unsafePerformIO
```

### Lamentations of `NULL`

**doobie** maps nullable columns via `Option`, so `null` is never observed in programs that use the high-level API, and the typechecking feature discussed earlier will find mismatches. So this means if you have a nullable SQL `varchar[]` then you will be chided grimly if you don't map it as `Option[List[String]]` (or some other supported sequence type).

However there is another axis of variation here: the *array cells* themselves may contain null values. And the query checker can't save you here because this is not reflected in the metadata provided by PostgreSQL or H2, and probably not by anyone.

So there are actually four ways to map an array, and you should carefully consider which is appropriate for your schema. In the first two cases reading a `NULL` cell would result in a `NullableCellRead` exception.

```tut
sql"select array['foo','bar','baz']".query[List[String]].quick.unsafePerformIO
sql"select array['foo','bar','baz']".query[Option[List[String]]].quick.unsafePerformIO
sql"select array['foo',NULL,'baz']".query[List[Option[String]]].quick.unsafePerformIO
sql"select array['foo',NULL,'baz']".query[Option[List[Option[String]]]].quick.unsafePerformIO
```

#+scalaz
### Diving Deep

We can easily add support for other sequence types like `scalaz.IList` by invariant mapping. The `nxmap` method is a variant of `xmap` that ensures null values read from the database are never observed. The `TypeTag` is required to provide better feedback when type mismatches are detected.

```tut:silent
import scala.reflect.runtime.universe.TypeTag

implicit def IListMeta[A: TypeTag](implicit ev: Meta[List[A]]): Meta[IList[A]] =
  ev.nxmap[IList[A]](IList.fromList, _.toList)
```

Once this mapping is in scope we can map columns directly to `IList`.

```tut
sql"select pets from person where name = 'Bob'".query[IList[String]].quick.unsafePerformIO
```
#-scalaz
