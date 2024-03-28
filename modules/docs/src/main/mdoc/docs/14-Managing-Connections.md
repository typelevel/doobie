## Managing Connections

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

Most **doobie** programs are values of type `ConnectionIO[A]` or `Stream[ConnectionIO, A]` that describe computations requiring a database connection. By providing a means of acquiring a JDBC connection we can transform these programs into computations that can actually be executed. The most common way of performing this transformation is via a `Transactor`.

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

### About threading

Throughout our doobie program we will run both non-blocking CPU-bound tasks or blocking JDBC operations.
Both of these will be handled by the Cats-Effect runtime.

Requesting and waiting for a connection from the connection pool is a blocking operation too, but this has to be handled by the `connectEC` ExecutionContext.
This ExecutionContext should be **bounded**, as we do not want to create hundreds and thousands of threads (one for each request) waiting for database connection when the database is busy.
The maximum thread limit for `connectEC` should be the same as your underlying JDBC connection pool, since any more threads are guaranteed to be blocked.
You probably won't need to create your own `connectEC` though because we can derive it from the configuration of the connection pool, such as when you use `HikariTransactor.fromHikariConfig`.

Because these pools need to be shut down in order to exit cleanly it is typical to use `Resource` to manage their lifetimes. See below for examples.

### Using a HikariCP Connection Pool

The `doobie-hikari` add-on provides a `Transactor` implementation backed by a [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pool. The connection pool is a lifetime-managed object that must be shut down cleanly, so it is managed as a `Resource`. A program that uses `HikariTransactor` will typically use `IOApp`.

```scala mdoc:silent:reset
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.hikari._
import com.zaxxer.hikari.HikariConfig

object HikariApp extends IOApp {

  // Resource yielding a transactor configured with a bounded connect EC and an unbounded
  // transaction EC. Everything will be closed and shut down cleanly after use.
  val transactor: Resource[IO, HikariTransactor[IO]] =
    for {
      hikariConfig <- Resource.pure {
        // For the full list of hikari configurations see https://github.com/brettwooldridge/HikariCP#gear-configuration-knobs-baby
        val config = new HikariConfig()
        config.setDriverClassName("org.h2.Driver")
        config.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
        config.setUsername("sa")
        config.setPassword("")
        config
      }
      xa <- HikariTransactor.fromHikariConfig[IO](hikariConfig)
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

```scala mdoc:silent
HikariApp.main(Array())
```

A runnable example with Hikari + Postgres can be found [here](https://github.com/tpolecat/doobie/blob/main/modules/example/src/main/scala/example/HikariExample.scala)

### Using the JDBC DriverManager

JDBC provides a bare-bones connection provider via `DriverManager.getConnection`, which has the advantage of being extremely simple: there is no connection pooling and thus no configuration required. The disadvantage is that it is quite a bit slower than pooling connection managers, and provides no upper bound on the number of concurrent connections. It executes blocking operations on a similar unbounded pool of daemon threads.

However, for test and for experimentation as described in this book (and for situations where you really do want to ensure that you get a truly fresh connection right away) the `DriverManager` is fine. Support in **doobie** is via `DriverManagerTransactor`. To construct one you must pass the name of the driver class and a connect URL. Normally you will also pass a user/password (the API provides several variants matching the `DriverManager` static API).

```scala mdoc:silent
import doobie.util.ExecutionContexts

// This is just for testing. Consider using cats.effect.IOApp instead of calling
// unsafe methods directly.
import cats.effect.unsafe.implicits.global

// A transactor that gets connections from java.sql.DriverManager and executes blocking operations
// on an our synchronous EC. See the chapter on connection handling for more info.
val xa = Transactor.fromDriverManager[IO](
  driver = "org.postgresql.Driver",  // JDBC driver classname
  url = "jdbc:postgresql:world",     // Connect URL - Driver specific
  user = "postgres",                 // Database user name
  password = "password",             // Database password
  logHandler = None                  // Don't setup logging for now. See Logging page for how to log events in detail
)
```

```scala mdoc:invisible
implicit val mdocColors: doobie.util.Colors = doobie.util.Colors.None
```

### Using an existing DataSource

If your application exposes an existing `javax.sql.DataSource` you can use it directly by wrapping it in a `DataSourceTransactor`. You still need to provide execution contexts.

```scala mdoc:silent
import javax.sql.DataSource

// Resource yielding a DataSourceTransactor[IO] wrapping the given `DataSource`
def transactor(ds: DataSource): Resource[IO, DataSourceTransactor[IO]] =
  for {
    ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
  } yield Transactor.fromDataSource[IO](ds, ce)
```

The `configure` method on `DataSourceTransactor` provides access to the underlying `DataSource` if additional configuration is required.


### Using an Existing JDBC Connection

If your application exposes an existing `Connection` you can use it directly by wrapping it in a `Transactor`. You still need to provide an execution context for blocking operations. Note that you are responsible for closing the `Connection`.

```scala mdoc:silent
import java.sql.Connection

// A Transactor[IO] wrapping the given `Connection`
def transactor(c: Connection): Transactor[IO] =
  Transactor.fromConnection[IO](c, logHandler = None)
```

### Customizing Transactors

If the default `Transactor` behavior don't meet your needs you can replace any member with one that does what you need. See the Scaladoc for `Transactor` and `Strategy` for details on the structure. Lenses are provided to make it straightforward to replace just the piece you're interested in. For example, to create a transactor that is the same as `xa` but always rolls back (for testing perhaps) you can say:

```scala
val testXa = Transactor.after.set(xa, HC.rollback)
```

As another example, Hive's JDBC driver doesn't support transaction commit or rollback, you can create your own  `Transactor` to accommodate that, like:

```scala
import doobie.free.connection.unit

val hiveXa = Transactor.strategy.set(xa, Strategy.default.copy(after = unit, oops = unit))
```
