---
layout: book
number: 7
title: DDL, Inserting, and Updating
---


### Setting Up

Again we set up an H2 transactor and pull in YOLO mode, but this time we're not using the world database.

```tut:silent
import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
import xa.yolo._
```

### Data Definition

Let's create a new table, which we will use for the examples to follow. This looks a lot like our prior usage of the `sql` interpolator, but this time we're using `update` rather than `query`. The types are indicated.

```tut:silent
val drop: ConnectionIO[Int] = 
  sql"""
    DROP TABLE IF EXISTS person
  """.update.run

val create: ConnectionIO[Int] = 
  sql"""
    CREATE TABLE person (
      id   SERIAL,
      name VARCHAR NOT NULL UNIQUE,
      age  SMALLINT
    )
  """.update.run
```

We can compose these and run them together.

```tut
(drop *> create).quick.run
```


### Inserting


Inserting is straightforward and works just as with selects.

```tut
def insert1(name: String, age: Option[Short]): Update0 =
  sql"insert into person (name, age) values ($name, $age)".update
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


Updating follows the same pattern.

```tut
sql"update person set age = 15 where name = 'Alice'".update.quick.run
sql"select id, name, age from person".query[Person].quick.run
```

### Retrieving Results

Of course when we insert we usually want the row back, so let's do that. First we'll do it the hard way, by inserting, getting the last used key via H2's `identity()` function, then selecting the indicated row. 

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

This is irritating but it is supported by all databases (although the "get the last used id" function will vary by vendor). A nicer way to do this is in one shot by returning specified columns from the inserted row. PostgreSQL supports this feature.

```tut:silent
def insert3(name: String, age: Option[Short]): ConnectionIO[Person] = {
  sql"insert into person (name, age) values ($name, $age)"
    .update
    .withUniqueGeneratedKeys("id", "name", "age")
}
```

```tut
insert3("Elvis", None).quick.run
```

This mechanism also works for updates, for databases that support it. In the case of multiple row updates we use `.withGeneratedKeys[A](cols...)` to get a `Process[ConnectionIO, A]`.


```
TODO
```




