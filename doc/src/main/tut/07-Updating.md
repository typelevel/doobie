---
layout: book
number: 7
title: DDL, Inserting, and Updating
---

In this chapter we examine operations that modify data in the database, and ways to retrieve the results of these updates.

### Setting Up

Again we set up a transactor and pull in YOLO mode, but this time we're not using the world database.

```tut:silent
import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
import xa.yolo._
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
(drop.quick *> create.quick).run
```


### Inserting


Inserting is straightforward and works just as with selects. Here we define a method that constructs an `Update0` that inserts a row into the `person` table.

```tut:silent
def insert1(name: String, age: Option[Short]): Update0 =
  sql"insert into person (name, age) values ($name, $age)".update
```

Let's insert a few rows.

```tut
insert1("Alice", Some(12)).quick.run
insert1("Bob", None).quick.run
```

And read them back.

```tut:silent
case class Person(id: Long, name: String, age: Option[Short])
```

```tut
sql"select id, name, age from person".query[Person].quick.run
```


### Updating


Updating follows the same pattern. Here we update Alice's age.

```tut
sql"update person set age = 15 where name = 'Alice'".update.quick.run
sql"select id, name, age from person".query[Person].quick.run
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
insert2("Jimmy", Some(42)).quick.run
```

This is irritating but it is supported by all databases (although the "get the last used id" function will vary by vendor). A nicer way to do this is in one shot by returning specified columns from the inserted row. Not all databases support this feature, but PostgreSQL does.

```tut:silent
def insert3(name: String, age: Option[Short]): ConnectionIO[Person] = {
  sql"insert into person (name, age) values ($name, $age)"
    .update.withUniqueGeneratedKeys("id", "name", "age")
}
```

The `withUniqueGeneratedKeys` specifies that we expect exactly one row back (otherwise an exception will be thrown), and requires a list of columns to return. This isn't the most beautiful API but it's what JDBC gives us. And it does work.

```tut
insert3("Elvis", None).quick.run
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
up.quick.run
up.quick.run // and again!
```






