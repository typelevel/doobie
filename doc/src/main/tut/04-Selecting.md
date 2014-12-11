---
layout: book
number: 4
title: Selecting Data
---

### Setting Up

First let's get our imports out of the way and set up a `Transactor` as in the previous chapter. This is an in-memory database, by the way, so there won't be anything left behind when we exit the VM.

```tut:silent
import doobie.imports._
import scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.h2.Driver",                      // driver class
  "jdbc:h2:mem:ch4;DB_CLOSE_DELAY=-1", // connect URL
  "sa", ""                              // user and pass
)
```

Now let's load up the sample database at the root of the **doobie** source tree. You can go grab it from github if you want to play along at home. The result is the total number of inserted rows.

```tut
sql"RUNSCRIPT FROM 'world.sql' CHARSET 'UTF-8'".update.run.transact(xa).run
```

Ok we now have some data out there that we can start messing with. Just for reference, the table we're playing with looks like this:

```sql
CREATE TABLE country (
  code character(3)  NOT NULL,
  name text          NOT NULL,
  population integer NOT NULL,
  gnp numeric(10,2)
  -- more columns, but we won't use them here
)
```

### Elementary Streaming

Let's aim low and select some country names into a `List` and print out the first few. The parens are just to make the REPL understand what's going on. We'll fix tut at some point.

```tut
(sql"select name from country"
  .query[String] // Query0[String]
  .list          // ConnectionIO[List[String]]
  .transact(xa)  // Task[List[String]]
  .run           // List[String]
  .take(5).foreach(println))
```

Let's break this down a bit.

- `sql"select name from country".query[String]` defines a `Query0[String]`, which is just a one-column query that maps to a `String`. We will get to more interesting row types soon.
- `.list` is a convenience method that streams the results, accumulating them in a `List[String]`, yielding a `ConnectionIO[String]`.
- The rest is familar; `transact(xa)` yields a `Task[List[String]]` which we run, giving us a normal Scala `List[String]` that we print out.

This is ok, but there's not much point streaming all the results from the database when we only want the first few rows. So let's try a different approach.

```tut
(sql"select name from country"
  .query[String] // Query0[String]
  .process       // Process[ConnectionIO, String]
  .take(5)       // Process[ConnectionIO, String]
  .list          // ConnectionIO[List[String]]
  .transact(xa)  // Task[List[String]]
  .run           // List[String]
  .foreach(println))
```

The difference here is that `process` gives us a `scalaz.stream.Process[ConnectionIO, String]` which emits the results as they arrive from the database. If we then `take(5)` from the process, we simply get a process that will shut everything down (and clean everything up) once five elements have been emitted. So this is much more efficient than pulling all 239 rows across. 

### YOLO Mode

Although this API is already fairly expressive, it's tiresome to keep saying `transact(xa)` and doing `foreach(println)` to see what the results look like. So *just for REPL exploration* there is a module of extra syntax provided on `Transactor` that you can import, and it gives you some shortcuts.

```tut:silent
import xa.yolo._
```

We can now run our previous query in an abbreviated form.

```tut
(sql"select name from country"
  .query[String] // Query0[String]
  .process       // Process[ConnectionIO, String]
  .take(5)       // Process[ConnectionIO, String]
  .quick         // Task[Unit]
  .run)
```

This syntax allows you to quickly run a `Query0[A]` or `Process[ConnectionIO, A]` and see the results printed to the console. This isn't a huge deal but it can save you some keystrokes when you're just messing around.

### Multi-Column Queries

We can select multiple columns, of course, and map them to a tuple. The `gnp` column is nullable so we'll select that one into an `Option[Double]`. In a later chapter we'll see how to check the types to be sure they're sensible.

```tut
sql"""
  select code, name, population, gnp 
  from country
""".query[(String, String, Int, Option[Double])].process.take(5).quick.run
```

But this is kind of lame; we really want a proper type. So let's define one.

```tut:silent
case class Country(code: String, name: String, pop: Int, gnp: Option[Double])
```

And try our query again, using `Country` rather than our tuple type.

```tut
sql"""
  select code, name, population, gnp 
  from country
""".query[Country].process.take(5).quick.run
```

Nesting also works fine, of course.

```tut:silent
case class Code(code: String)
case class Country(name: String, pop: Int, gnp: Option[Double])
```

```tut
sql"""
  select code, name, population, gnp 
  from country
""".query[(Code, Country)].process.take(5).quick.run
```

This kind of thing is useful for example if we want a `Map` rather than a `List` as our result. With types illustrated, for your convenience:

```tut
(sql"""
  select code, name, population, gnp 
  from country
""".query[(Code, Country)] // Query0[(Code, Country)]
   .process.take(5)        // Process[ConnectionIO, (Code, Country)]
   .list                   // ConnectionIO[List[(Code, Country)]]
   .map(_.toMap)           // ConnectionIO[Map[Code, Country]]
   .quick.run)
```










