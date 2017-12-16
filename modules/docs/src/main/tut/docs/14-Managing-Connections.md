---
layout: docs
number: 14
title: Managing Connections
---

## {{page.title}}

<div class="alert alert-warning" role="alert">
<b>Note:</b> Doobie 0.4.2 introduced a new <code>Transactor</code> design that makes it simple to customize the behavior and, combined with new interpreter design, makes it practical to use doobie types in free coproducts (see `coproduct.scala` in the `example` project).
</div>

In this chapter we discuss several ways to manage connections in applications that use **doobie**, including managed/pooled connections and re-use of existing connections. For this chapter we have a few imports and no other setup.

```tut:silent
import doobie._, doobie.implicits._
import cats._, cats.data._, cats.effect.IO, cats.implicits._
```

### About Transactors

Most **doobie** programs are values of type `ConnectionIO[A]` or `Process[ConnnectionIO, A]` that describe computations requiring a database connection. By providing a means of acquiring a connection we can transform these programs into computations that can actually be executed. The most common way of performing this transformation is via a `Transactor`.

A `Transactor.Aux[M, A]` closes over some source of connections and configuration information (`A`). Based on this, it provides several natural transformations from `ConnectionIO` to `M`, where `M[_]` is the target monad.

A `Strategy`, which represents the common setup, error-handling, and cleanup strategy associated with a SQL transaction, can also be configured for a `Transactor`, where sane defaults are provided. A `Transactor` uses a `Strategy` to wrap programs prior to execution.

These are the natural transformations that a `Transactor` provides:

- `trans: ConnectionIO ~> M` A natural transformation of a program in `ConnectionIO` to the target monad `M` that uses the given `Strategy` to wrap the given program with additional setup, error-handling and cleanup operations. This yields an independent program in `M`. This is the most common way to run a doobie program.
  - e.g., `xa.trans.apply(program1)`
  - you can also use the syntax `program1.transact(xa)`, which runs `xa.trans` under the hood
- `rawTrans` natural transformation equivalent to `trans` but one that does not use the provided `Strategy` to wrap the given program with additional operations. This can be useful in cases where transactional handling is unsupported or undesired.
- `rawTransP: Process[ConnectionIO, ?] ~> Process[M, ?]` equivalent to `rawTrans` but expressed using `Process` (which is an alias for `fs2.Stream`)
- `transP: Process[ConnectionIO, ?] ~> Process[M, ?]` equivalent to `trans` but expressed using `Process` (which is an alias for `fs2.Stream`)
streaming fashion
- `exec: Kleisli[M, Connection, ?] ~> M` equivalent to `trans` except it transforms a `Kleisli` that expects a `java.sql.Connection` and not a `ConnectionIO`. This can be used in combination with the doobie interpreters, which can transform doobie programs (e.g., `ConnectionIO`) to `Kleisli` effects, in order to implement your own logic for running doobie programs.
- `rawExec` natural transformation equivalent to `exec` but one that does not use the provided `Strategy` to wrap the given program with additional operations.

So summarizing, once you have a `Transactor[M, A]` you have a way of discharging `ConnectionIO` and replacing it with some effectful `M` like `IO`. In effect this turns a **doobie** program into a "real" program value that you can integrate with the rest of your application; all doobieness is left behind.

**doobie** provides several implementations, described below.

### Using the JDBC DriverManager

JDBC provides a bare-bones connection provider via `DriverManager.getConnection`, which has the advantage of being extremely simple: there is no connection pooling and thus no configuration required. The disadvantage is that it is quite a bit slower than pooling connection managers, and provides no upper bound on the number of concurrent connections.

However, for experimentation as described in this book (and for situations where you really do want to ensure that you get a truly fresh connection right away) the `DriverManager` is ideal. Support in **doobie** is via `DriverManagerTransactor`. To construct one you must pass the name of the driver
class and a connect URL. Normally you will also pass a user/password (the API provides several variants matching the `DriverManager` static API).

```tut:silent
val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", // fully-qualified driver class name
  "jdbc:postgresql:world", // connect URL
  "jimmy",                 // user
  "coconut"                // password
)
```

### Using a HikariCP Connection Pool

The `doobie-hikari` add-on provides a `Transactor` implementation backed by a [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pool. The connnection pool has internal state so constructing one is an effect:

```tut:silent
import doobie.hikari._, doobie.hikari.implicits._

val q = sql"select 42".query[Int].unique

val p: IO[Int] = for {
  xa <- HikariTransactor.newHikariTransactor[IO]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
  _  <- xa.configure(hx => IO( /* do something with hx */ ()))
  a  <- q.transact(xa) guarantee xa.shutdown
} yield a
```

And running this `IO` gives us the desired result.

```tut
p.unsafeRunSync
```

The returned instance is of type `HikariTransactor`, which provides a `shutdown` method, as well as a `configure` method that provides access to the underlying `HikariDataSource` if additional configuration is required.

### Using an existing DataSource

If your application exposes an existing `javax.sql.DataSource` you can use it directly by wrapping it in a `DataSourceTransactor`.

```tut:silent
val ds: javax.sql.DataSource = null // pretending

val xa = Transactor.fromDataSource[IO](ds)

val p: IO[Int] = for {
  _  <- xa.configure(ds => IO( /* do something with ds */ ()))
  a  <- q.transact(xa)
} yield a
```

The `configure` method on `DataSourceTransactor` provides access to the underlying `DataSource` if additional configuration is required.

### Customizing Transactors

If the default `Transactor` behavior don't meet your needs you can replace any member with one that does what you need. See the Scaladoc for `Transactor` and `Strategy` for details on the structure. Lenses are provided to make it straightforward to replace just the piece you're interested in. For example, to create a transactor that is the same as `xa` but always rolls back (for testing perhaps) you can say:

```tut
val testXa = Transactor.after.set(xa, HC.rollback)
```

### Using an Existing JDBC Connection

If you have an existing `Connection` you can transform a `ConnectionIO[A]` to an `M[A]` for any target monad `M` that has `Catchable` and `Capture` instances by running the `Kleisli[M, Connection, A]` yielded by the default interpreter.

```tut:silent
val conn: java.sql.Connection = null     // Connection (pretending)
val prog = 42.pure[ConnectionIO]         // ConnectionIO[Int]
val int  = KleisliInterpreter[IO]    // KleisliInterpreter[IO]
val nat  = int.ConnectionInterpreter     // ConnectionIO ~> Kleisli[IO, Connection, ?]
val task = prog.foldMap(nat).run(conn)   // IO[Int]
```

As an aside, this technique works for programs written in *any* of the provided contexts. For example, here we run a program in `ResultSetIO`.

```tut:silent
val rs: java.sql.ResultSet = null      // ResultSet (pretending)
val prog = 42.pure[ResultSetIO]        // ResultSetIO[Int]
val nat  = int.ResultSetInterpreter    // ResultSetIO ~> Kleisli[IO, ResultSet, ?]
val task = prog.foldMap(nat).run(rs)   // IO[Int]
```

This facility allows you to mix **doobie** programs into existing JDBC applications in a fine-grained manner if this meets your needs.
