---
layout: docs
number: 14
title: Managing Connections
---

## {{page.title}}

In this chapter we discuss several ways to manage connections in applications that use **doobie**, including managed/pooled connections and re-use of existing connections. For this chapter we have a few imports and no other setup.

```scala mdoc:silent
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
```

### About Transactors

Most **doobie** programs are values of type `ConnectionIO[A]` or `Stream[ConnnectionIO, A]` that describe computations requiring a database connection. By providing a means of acquiring a JDBC connection we can transform these programs into computations that can actually be executed. The most common way of performing this transformation is via a `Transactor`.

A `Transactor[M]` consists of the following bits of information:

- An arbitrary piece of configuration, called the `kernel`, particular to a given implementation (see below);
- a source of connections, computed in `M` (typically `IO`);
- an *interpreter* from `ConnectionOp ~> M`; and
- a transaction `Strategy` that specifies a setup, error-handling, and cleanup strategy associated with each database interation.

Given this information a `Transactor[M]` can provide the following transformations:

- `trans: ConnectionIO ~> M` A natural transformation of a program in `ConnectionIO` to the target monad `M` that uses the given `Strategy` to wrap the given program with additional setup, error-handling and cleanup operations. This yields an independent program in `M`. This is the most common way to run a doobie program.
  - e.g., `xa.trans.apply(program1)`
  - you can also use the syntax `program1.transact(xa)`, which runs `xa.trans` under the hood
- `rawTrans` natural transformation equivalent to `trans` but one that does not use the provided `Strategy` to wrap the given program with additional operations. This can be useful in cases where transactional handling is unsupported or undesired.
- `rawTransP: Stream[ConnectionIO, ?] ~> Stream[M, ?]` equivalent to `rawTrans` but expressed using `Stream`.
- `transP: Stream[ConnectionIO, ?] ~> Stream[M, ?]` equivalent to `trans` but expressed using `Stream`.
- `exec: Kleisli[M, Connection, ?] ~> M` equivalent to `trans` except it transforms a `Kleisli` that expects a `java.sql.Connection` and not a `ConnectionIO`. This can be used in combination with the doobie interpreters, which can transform doobie programs (e.g., `ConnectionIO`) to `Kleisli` effects, in order to implement your own logic for running doobie programs.
- `rawExec` natural transformation equivalent to `exec` but one that does not use the provided `Strategy` to wrap the given program with additional operations.

So summarizing, once you have a `Transactor[M]` you have a way of discharging `ConnectionIO` and replacing it with some effectful `M` like `IO`. In effect this turns a **doobie** program into a "real" program value that you can integrate with the rest of your application; all doobieness is left behind.

### About Threading

Starting with version 0.6.0 **doobie** provides an asynchronous API that delegates blocking operations to dedicated execution contexts *if you use the provided `Transactor` implementations*. To construct any of the provided `Transactor[M]`s (other than `DriverManagerTransactor`) you need

- `ContextShift[M]`, which provides a CPU-bound pool for **non-blocking operations**. This is typically backed by `ExecutionContext.global`. If you use `IOApp` and interpret into `IO` this will be available for free.
- An `ExecutionContext` for **awaiting connection** to the database. Because there can be an unbounded number of connections awaiting database access this should be a **bounded** pool.
- A `cats.effect.Blocker` for **executing JDBC operations**. Because your connection pool limits the number of active connections this should be an **unbounded** pool.

Because these pools need to be shut down in order to exit cleanly it is typical to use `Resource` to manage their lifetimes. See below for examples.

### Using the JDBC DriverManager

JDBC provides a bare-bones connection provider via `DriverManager.getConnection`, which has the advantage of being extremely simple: there is no connection pooling and thus no configuration required. The disadvantage is that it is quite a bit slower than pooling connection managers, and provides no upper bound on the number of concurrent connections. It executes blocking operations on a similar unbounded pool of daemon threads.

However, for test and for experimentation as described in this book (and for situations where you really do want to ensure that you get a truly fresh connection right away) the `DriverManager` is fine. Support in **doobie** is via `DriverManagerTransactor`. To construct one you must pass the name of the driver class and a connect URL. Normally you will also pass a user/password (the API provides several variants matching the `DriverManager` static API).

```scala mdoc:silent
import doobie.util.ExecutionContexts

// We need a ContextShift[IO] before we can construct a Transactor[IO]. The passed ExecutionContext
// is where nonblocking operations will be executed. For testing here we're using a synchronous EC.
implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

// A transactor that gets connections from java.sql.DriverManager and executes blocking operations
// on an our synchronous EC. See the chapter on connection handling for more info.
val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver",     // driver classname
  "jdbc:postgresql:world",     // connect URL (driver-specific)
  "postgres",                  // user
  "",                          // password
  Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
)
```

```scala mdoc:invisible
implicit val mdocColors: doobie.util.Colors = doobie.util.Colors.None
```

### Using a HikariCP Connection Pool

The `doobie-hikari` add-on provides a `Transactor` implementation backed by a [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pool. The connection pool is a lifetime-managed object that must be shut down cleanly, so it is managed as a `Resource`. A program that uses `HikariTransactor` will typically use `IOApp`.

```scala mdoc:silent:reset
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.hikari._

object HikariApp extends IOApp {

  // Resource yielding a transactor configured with a bounded connect EC and an unbounded
  // transaction EC. Everything will be closed and shut down cleanly after use.
  val transactor: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      be <- Blocker[IO]    // our blocking EC
      xa <- HikariTransactor.newHikariTransactor[IO](
              "org.h2.Driver",                        // driver classname
              "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",   // connect URL
              "sa",                                   // username
              "",                                     // password
              ce,                                     // await connection here
              be                                      // execute JDBC operations here
            )
    } yield xa


  def run(args: List[String]): IO[ExitCode] =
    transactor.use { xa =>

      // Construct and run your server here!
      for {
        n <- sql"select 42".query[Int].unique.transact(xa)
        _ <- IO(println(n))
      } yield ExitCode.Success

    }

}
```

And running this program gives us the desired result.

```scala mdoc
HikariApp.main(Array())
```

### Using an existing DataSource

If your application exposes an existing `javax.sql.DataSource` you can use it directly by wrapping it in a `DataSourceTransactor`. You still need to provide execution contexts.

```scala mdoc:silent
import javax.sql.DataSource

// Resource yielding a DataSourceTransactor[IO] wrapping the given `DataSource`
def transactor(ds: DataSource)(
  implicit ev: ContextShift[IO]
): Resource[IO, DataSourceTransactor[IO]] =
  for {
    ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
    be <- Blocker[IO]    // our blocking EC
  } yield Transactor.fromDataSource[IO](ds, ce, be)
```

The `configure` method on `DataSourceTransactor` provides access to the underlying `DataSource` if additional configuration is required.


### Using an Existing JDBC Connection

If your application exposes an existing `Connection` you can use it directly by wrapping it in a `Transactor`. You still need to provide an execution context for blocking operations. Note that you are responsible for closing the `Connection`.

```scala mdoc:silent
import java.sql.Connection

// Resource yielding a Transactor[IO] wrapping the given `Connection`
def transactor(c: Connection)(
  implicit ev: ContextShift[IO]
): Resource[IO, Transactor[IO]] =
  Blocker[IO].map { b =>
    Transactor.fromConnection[IO](c, b)
  }
```

### Customizing Transactors

If the default `Transactor` behavior don't meet your needs you can replace any member with one that does what you need. See the Scaladoc for `Transactor` and `Strategy` for details on the structure. Lenses are provided to make it straightforward to replace just the piece you're interested in. For example, to create a transactor that is the same as `xa` but always rolls back (for testing perhaps) you can say:

```scala
val testXa = Transactor.after.set(xa, HC.rollback)
```

As another exmaple, Hive's JDBC driver doesn't support transaction commit or rollback, you can create your own  `Transactor` to accommodate that, like:

```scala
import doobie.free.connection.unit

val hiveXa = Transactor.strategy.set(xa, Strategy.default.copy(after = unit, oops = unit))
```
