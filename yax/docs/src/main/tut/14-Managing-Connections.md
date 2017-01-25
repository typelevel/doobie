---
layout: book
number: 14
title: Managing Connections
---

<div class="alert alert-warning" role="alert">
<b>Note:</b> The <code>Transactor</code> type is being generalized for <b>doobie</b> 0.3.0 and there will be minor API changes as a result. In particular transactors will be configurable generically, which will make custom implementations less common.
</div>

In this chapter we discuss several ways to manage connections in applications that use **doobie**, including managed/pooled connections and re-use of existing connections. For this chapter we have a few imports and no other setup.

```tut:silent
import doobie.imports._
#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.data._, cats.implicits._
import fs2.interop.cats._
#-cats
```

### About Transactors

Most **doobie** programs are values of type `ConnectionIO[A]` or `Process[ConnnectionIO, A]` that describe computations requiring a database connection. By providing a means of acquiring a connection we can transform these programs into computations that can actually be executed. The most common way of performing this transformation is via a `Transactor`.

A `Transactor` is a parameterized by some target monad `M` and closes over some source of connections (and configuration information, as needed) yielding a pair of natural transformations:

```scala
ConnectionIO ~> M
Process[ConnectionIO, ?] ~> Process[M, ?]
```

So once you have a `Transactor[M]` you have a way of discharging `ConnectionIO` and replacing it with some effectful `M` like `Task` or `IO`. In effect this turns a **doobie** program into a "real" program value that you can integrate with the rest of your application; all doobieness is left behind.

In addition to simply supplying a connection, a `Transactor` (by default) wraps the transformed `ConnectionIO` as follows:

- The connection is configured with `setAutoCommit(false)`
- The program is followed by `commit` if it completes normally, or `rollback` is an exception escapes.
- In all cases the connection is cleaned up with `close`.

**doobie** provides several implementations, described below.

### Using the JDBC DriverManager

JDBC provides a bare-bones connection provider via `DriverManager.getConnection`, which has the advantage of being extremely simple: there is no connection pooling and thus no configuration required. The disadvantage is that it is quite a bit slower than pooling connection managers, and provides no upper bound on the number of concurrent connections.

However, for experimentation as described in this book (and for situations where you really do want to ensure that you get a truly fresh connection right away) the `DriverManager` is ideal. Support in **doobie** is via `DriverManagerTransactor`. To construct one you must pass the name of the driver
class and a connect URL. Normally you will also pass a user/password (the API provides several variants matching the `DriverManager` static API).

```tut:silent
val xa = DriverManagerTransactor[IOLite](
  "org.postgresql.Driver", // fully-qualified driver class name
  "jdbc:postgresql:world", // connect URL
  "jimmy",                 // user
  "coconut"                // password
)
```

### Using a HikariCP Connection Pool

The `doobie-hikari-cats` add-on provides a `Transactor` implementation backed by a [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pool. The connnection pool has internal state so constructing one is an effect:

```tut:silent
import doobie.hikari.imports._

val q = sql"select 42".query[Int].unique

val p: IOLite[Int] = for {
  xa <- HikariTransactor[IOLite]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
  _  <- xa.configure(hx => IOLite.primitive( /* do something with hx */ ()))
  a  <- q.transact(xa) ensuring xa.shutdown
} yield a
```

And running this `IOLite` gives us the desired result.

```tut
p.unsafePerformIO
```

The returned instance is of type `HikariTransactor`, which provides a `shutdown` method, as well as a `configure` method that provides access to the underlying `HikariDataSource` if additional configuration is required.

### Using an existing DataSource

If your application exposes an existing `javax.sql.DataSource` you can use it directly by wrapping it in a `DataSourceTransactor`.

```tut:silent
val ds: javax.sql.DataSource = null // pretending

val xa = DataSourceTransactor[IOLite](ds)

val p: IOLite[Int] = for {
  _  <- xa.configure(ds => IOLite.primitive( /* do something with ds */ ()))
  a  <- q.transact(xa)
} yield a

```

The `configure` method on `DataSourceTransactor` provides access to the underlying `DataSource` if additional configuration is required.

### Building your own Transactor

If the provided `Transactor` implementations don't meet your needs, it is straightforward to build your own using any connection provider. At a minimum all you need to do is implement the `connect` method, which returns a [logically] fresh connection lifted into a target monad. See the source for existing implementations; it's likely that you can copy/paste your way to a custom `Transactor` without much trouble.

### Using an Existing JDBC Connection

If you have an existing `Connection` you can transform a `ConnectionIO[A]` to an `M[A]` for any target monad `M` that has `Catchable` and `Capture` instances by running the `Kleisli[M, Connection, A]` yielded by the default interpreter.

```tut:silent
val conn: java.sql.Connection = null     // Connection (pretending)
val prog = 42.pure[ConnectionIO]         // ConnectionIO[Int]
val int  = KleisliInterpreter[IOLite]    // KleisliInterpreter[IOLite]
val nat  = int.ConnectionInterpreter     // ConnectionIO ~> Kleisli[IOLite, Connection, ?]
val task = prog.foldMap(nat).run(conn)   // IOLite[Int]
```

As an aside, this technique works for programs written in *any* of the provided contexts. For example, here we run a program in `ResultSetIO`.

```tut:silent
val rs: java.sql.ResultSet = null      // ResultSet (pretending)
val prog = 42.pure[ResultSetIO]        // ResultSetIO[Int]
val nat  = int.ResultSetInterpreter    // ResultSetIO ~> Kleisli[IOLite, ResultSet, ?]
val task = prog.foldMap(nat).run(rs)   // IOLite[Int]
```

This facility allows you to mix **doobie** programs into existing JDBC applications in a fine-grained manner if this meets your needs.
