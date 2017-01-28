---
layout: book
number: 7
title: DDL, Inserting, and Updating
---

In this chapter we examine operations that modify data in the database, and ways to retrieve the results of these updates.

### Setting Up

Again we set up a transactor and pull in YOLO mode, but this time we're not using the world database.

```tut:silent
import doobie.imports._
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

### Data Definition

It is uncommon to define database structures at runtime, but **doobie** handles it just fine and treats such operations like any other kind of update. And it happens to be useful here!

Let's create a new table, which we will use for the examples to follow. This looks a lot like our prior usage of the `sql` interpolator, but this time we're using `update` rather than `query`. The `.run` method gives a `ConnectionIO[Int]` that yields the total number of rows modified, and the YOLO-mode `.quick` gives a `Task[Unit]` that prints out the row count.

```tut:silent
val drop: Update0 =
  sql"""
    DROP TABLE IF EXISTS person
  """.update

val create: Update0 =
  sql"""
    CREATE TABLE person (
      id   SERIAL,
      name VARCHAR NOT NULL UNIQUE,
      age  SMALLINT
    )
  """.update
```

We can compose these and run them together.

```tut
(drop.run *> create.run).transact(xa).unsafePerformIO
```


### Inserting


Inserting is straightforward and works just as with selects. Here we define a method that constructs an `Update0` that inserts a row into the `person` table.

```tut:silent
def insert1(name: String, age: Option[Short]): Update0 =
  sql"insert into person (name, age) values ($name, $age)".update
```

Let's insert a few rows.

```tut
insert1("Alice", Some(12)).run.transact(xa).unsafePerformIO
insert1("Bob", None).quick.unsafePerformIO // switch to YOLO mode
```

And read them back.

```tut:silent
case class Person(id: Long, name: String, age: Option[Short])
```

```tut
sql"select id, name, age from person".query[Person].quick.unsafePerformIO
```


### Updating


Updating follows the same pattern. Here we update Alice's age.

```tut
sql"update person set age = 15 where name = 'Alice'".update.quick.unsafePerformIO
sql"select id, name, age from person".query[Person].quick.unsafePerformIO
```

### Retrieving Results

When we insert we usually want the new row back, so let's do that. First we'll do it the hard way, by inserting, getting the last used key via `lastVal()`, then selecting the indicated row.

```tut:silent
def insert2(name: String, age: Option[Short]): ConnectionIO[Person] =
  for {
    _  <- sql"insert into person (name, age) values ($name, $age)".update.run
    id <- sql"select lastval()".query[Long].unique
    p  <- sql"select id, name, age from person where id = $id".query[Person].unique
  } yield p
```

```tut
insert2("Jimmy", Some(42)).quick.unsafePerformIO
```

This is irritating but it is supported by all databases (although the "get the last used id" function will vary by vendor).

Some database (like H2) allow you to return [only] the inserted id, allowing the above operation to be reduced to two statements (see below for an explanation of `withUniqueGeneratedKeys`).

```tut:silent
def insert2_H2(name: String, age: Option[Short]): ConnectionIO[Person] =
  for {
    id <- sql"insert into person (name, age) values ($name, $age)".update.withUniqueGeneratedKeys[Int]("id")
    p  <- sql"select id, name, age from person where id = $id".query[Person].unique
  } yield p
```

```tut
insert2_H2("Ramone", Some(42)).quick.unsafePerformIO
```

Other databases (including PostgreSQL) provide a way to do this in one shot by returning multiple specified columns from the inserted row.

```tut:silent
def insert3(name: String, age: Option[Short]): ConnectionIO[Person] = {
  sql"insert into person (name, age) values ($name, $age)"
    .update.withUniqueGeneratedKeys("id", "name", "age")
}
```

The `withUniqueGeneratedKeys` specifies that we expect exactly one row back (otherwise an exception will be thrown), and requires a list of columns to return. This isn't the most beautiful API but it's what JDBC gives us. And it does work.

```tut
insert3("Elvis", None).quick.unsafePerformIO
```

This mechanism also works for updates, for databases that support it. In the case of multiple row updates we omit `unique` and get a `Process[ConnectionIO, Person]` back.


```tut:silent
val up = {
  sql"update person set age = age + 1 where age is not null"
    .update.withGeneratedKeys[Person]("id", "name", "age")
}
```

Running this process updates all rows with a non-`NULL` age and returns them.

```tut
up.quick.unsafePerformIO
up.quick.unsafePerformIO // and again!
```

### Batch Updates

**doobie** supports batch updating via the `updateMany` and `updateManyWithGeneratedKeys` operations on the `Update` data type (which we haven't seen before). An `Update0`, which is the type of an `sql"..."` expression, represents a parameterized statement where the arguments are known. An `Update[A]` is more general, and represents a parameterized statement where the composite argument of type `A` is *not* known.

```tut:silent
// Given some values ...
val a = 1; val b = "foo"

// this expression ...
sql"... $a $b ..."

// is syntactic sugar for this one, which is an Update applied to (a, b)
Update[(Int, String)]("... ? ? ...").run((a, b))
```

By using an `Update` directly we can apply *many* sets of arguments to the same statement, and execute it as a single batch operation.

```tut:silent
type PersonInfo = (String, Option[Short])

def insertMany(ps: List[PersonInfo]): ConnectionIO[Int] = {
  val sql = "insert into person (name, age) values (?, ?)"
  Update[PersonInfo](sql).updateMany(ps)
}

// Some rows to insert
val data = List[PersonInfo](
  ("Frank", Some(12)),
  ("Daddy", None))
```

Running this program yields the number of updated rows.

```tut
insertMany(data).quick.unsafePerformIO
```

For databases that support it (such as PostgreSQL) we can use `updateManyWithGeneratedKeys` to return a stream of updated rows.

```tut:silent
#+scalaz
import scalaz.stream.Process
#-scalaz
#+fs2
import fs2.Stream
#-fs2

#+scalaz
def insertMany2(ps: List[PersonInfo]): Process[ConnectionIO, Person] = {
#-scalaz
#+cats
def insertMany2(ps: List[PersonInfo]): Stream[ConnectionIO, Person] = {
#-cats
  val sql = "insert into person (name, age) values (?, ?)"
  Update[PersonInfo](sql).updateManyWithGeneratedKeys[Person]("id", "name", "age")(ps)
}

// Some rows to insert
val data = List[PersonInfo](
  ("Banjo",   Some(39)),
  ("Skeeter", None),
  ("Jim-Bob", Some(12)))
```

Running this program yields the updated instances.

```tut
insertMany2(data).quick.unsafePerformIO
```
