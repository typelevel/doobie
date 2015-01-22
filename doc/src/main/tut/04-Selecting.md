---
layout: book
number: 4
title: Selecting Data
---

In this chapter we construct some programs that retrieve data from the database and stream it back, mapping to Scala types on the way. We also introduce YOLO mode for experimenting with **doobie** in the REPL.

### Setting Up

First let's get our imports out of the way and set up a `Transactor` as we did before. You can skip this step if you still have your REPL running from last chapter.

```tut:silent
import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
```

We will be playing with the `country` table, shown here for reference.

```sql
CREATE TABLE country (
  code       character(3)  NOT NULL,
  name       text          NOT NULL,
  population integer       NOT NULL,
  gnp        numeric(10,2)
  -- more columns, but we won't use them here
)
```

### Elementary Streaming

For our first query let's aim low and select some country names into a `List`, then print out the first few. There are several steps here so we have noted the types along the way.

```tut
(sql"select name from country"
  .query[String] // Query0[String]
  .list          // ConnectionIO[List[String]]
  .transact(xa)  // Task[List[String]]
  .run           // List[String]
  .take(5).foreach(println))
```

Let's break this down a bit.

- `sql"select name from country".query[String]` defines a `Query0[String]`, which is a one-column query that maps each returned row to a `String`. We will get to more interesting row types soon.
- `.list` is a convenience method that streams the results, accumulating them in a `List`, in this case yielding a `ConnectionIO[List[String]]`.
- The rest is familar; `transact(xa)` yields a `Task[List[String]]` which we run, giving us a normal Scala `List[String]` that we print out.

This is ok, but there's not much point reading all the results from the database when we only want the first few rows. So let's try a different approach.

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

The difference here is that `process` gives us a `scalaz.stream.Process[ConnectionIO, String]` which emits the results as they arrive from the database. By applying `take(5)` we instruct the process to shut everything down (and clean everything up) after five elements have been emitted. This is much more efficient than pulling all 239 rows and then throwing most of them away.

Of course a server-side `LIMIT` would be an even better way to do this (for databases that support it), but in cases where you need client-side filtering or other custom postprocessing, `Process` is a very general and powerful tool. For more information see the [scalaz-stream](https://github.com/scalaz/scalaz-stream) site, which has a good list of learning resources. 

### YOLO Mode

The API we have seen so far is ok, but it's tiresome to keep saying `transact(xa)` and doing `foreach(println)` to see what the results look like. So **just for REPL exploration** there is a module of extra syntax provided on `Transactor` that you can import, and it gives you some shortcuts.

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

We can select multiple columns, of course, and map them to a tuple. The `gnp` column in our table is nullable so we'll select that one into an `Option[Double]`. In a later chapter we'll see how to check the types to be sure they're sensible.

```tut
(sql"select code, name, population, gnp from country"
  .query[(String, String, Int, Option[Double])]
  .process.take(5).quick.run)
```
**doobie** automatically supports row mappings for atomic column types, as well as options, tuples, and case classes thereof. So let's try the same query, mapping rows to a case class.

```tut:silent
case class Country(code: String, name: String, pop: Int, gnp: Option[Double])
```

```tut
(sql"select code, name, population, gnp from country"
  .query[Country] // Query0[Country]
  .process.take(5).quick.run)
```

You can also nest case classes and/or tuples arbitrarily as long as the eventual members are of supported columns types. For instance, here we map the same set of columns to a tuple of two case classes:

```tut:silent
case class Code(code: String)
case class Country(name: String, pop: Int, gnp: Option[Double])
```

```tut
(sql"select code, name, population, gnp from country"
  .query[(Code, Country)] // Query0[(Code, Country)]
  .process.take(5).quick.run)
```

And just for fun, since the `Code` values are constructed from the primary key, let's turn the results into a `Map`. Trivial but useful.

```tut
(sql"select code, name, population, gnp from country"
   .query[(Code, Country)] // Query0[(Code, Country)]
   .process.take(5)        // Process[ConnectionIO, (Code, Country)]
   .list                   // ConnectionIO[List[(Code, Country)]]
   .map(_.toMap)           // ConnectionIO[Map[Code, Country]]
   .quick.run)
```


### Diving Deeper

The `sql` interpolator is sugar for constructors defined in the `doobie.hi.connection` module, aliased as `HC` if you use the standard imports. Using these constructors directly, the above program would look like this:

```tut:silent

val sql = "select code, name, population, gnp from country"

val proc = HC.process[(Code, Country)](sql, ().point[PreparedStatementIO])

(proc.take(5)        // Process[ConnectionIO, (Code, Country)]
     .list           // ConnectionIO[List[(Code, Country)]]
     .map(_.toMap)   // ConnectionIO[Map[Code, Country]]
     .quick.run)
```

The `process` combinator is parameterized on the process element type and consumes a sql statement and a program in `PreparedStatementIO` that sets input parameters and any other pre-execution configuration. In this case the "prepare" program is a no-op.





