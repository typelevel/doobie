---
layout: book
number: 12
title: Managing Connections
---

In this chapter we discuss several ways to manage connections in applications that use **doobie**, including managed/pooled connections and re-use of existing connections. For this chapter we have a few imports and no other setup.

```tut:silent
import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task
```

### Using Transactors

Most **doobie** programs are values of type `ConnectionIO[A]` that describe computations requiring a database connection. By providing a means of acquiring a connection we can transform these programs into computations that can be executed. The most common way of performing this transformation is via a `Transactor`.


The `transact` method does the following...



#### Using the JDBC DriverManager

JDBC provides a bare-bones connection provider via `DriverManager.getConnection`, which has the advantage of being extremely simple: there is no connection pooling and thus no configuration required. The disadvantage is that it is quite a bit slower than pooling connection managers, and provides no upper bound on the number of concurrent connections.

However, for experimentation as described in this book, the `DriverManager` is ideal. Support in **doobie** is via `DriverManagerTransactor`.



#### Using a HikariCP Connection Pool

The `doobie-contrib-hikari` add-on provides a `Transactor` implementation backed by a [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pool. The connnection pool has internal state so constructing one is an effect:

```tut:silent
import doobie.contrib.hikari.hikaritransactor._

val q = sql"select 42".query[Int].unique

val p: Task[Int] = for {
  xa <- HikariTransactor[Task]("jdbc:postgresql:world", "postgres", "")
  a  <- q.transact(xa) ensuring xa.shutdown
} yield a
```

And running this `Task` gives us the desired result.

```tut
p.run
```

The returned instance is of type `HikariTransactor`, which provides a `shutdown` method (shown above) as well as a `configure` method that provides access to the underlying `HikariDataSource`. See the source for details.


#### Using an existing DataSource



#### Building your own Transactor

Building your own `Transactor` to wrap a connection pool is usually quite simple ... show an example


### Using an Existing JDBC Connection

If you have an existing `Connection` you can transform a `ConnectionIO[A]` to an `M[A]` for any target monad `M` that has `Catchable` and `Capture` instances by running the `Kleisli[M, Connection, A]` provided by the `transK` method.

```tut
val conn: java.sql.Connection = null   // Connection (pretending)
val prog = 42.point[ConnectionIO]      // ConnectionIO[Int]
val task = prog.transK[Task].run(conn) // Task[Int]
```

As an aside, this technique works for programs written in *any* of the provided contexts. For example, here we run a program in `ResultSetIO`.

```tut
val rs: java.sql.ResultSet = null    // ResultSet (pretending)
val prog = 42.point[ResultSetIO]     // ResultSetIO[Int]
val task = prog.transK[Task].run(rs) // Task[Int]
```

This facility allows you to mix **doobie** programs into existing JDBC applications in a fine-grained manner if this meets your needs.






