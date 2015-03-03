---
layout: book
number: 12
title: Managing Connections
---



```tut:silent
import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task
```

### Using Transactors

A `Transactor` is ...

The `transact` method does the following...



#### Using the JDBC DriverManager

JDBC provides a bare-bones connection provider via `DriverManager.getConnection`, which has the advantage of being extremely simple: there is no connection pooling and thus no configuration required. The disadvantage is that it is quite a bit slower than pooling connection managers, and provides no upper bound on the number of concurrent connections.

However, for experimentation as described in this book, the `DriverManager` is ideal. Support in **doobie** is via `DriverManagerTransactor`.



#### Using a HikariCP Connection Pool

The `doobie-contrib-hikari` add-on provides a `Transactor` implementation backed by a [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pool. Constructing an instance is an effect:

```scala
for {
  xa <- HikariTransactor[Task]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
  _  <- longRunningProgram(xa) ensuring xa.shutdown
} yield ()
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






