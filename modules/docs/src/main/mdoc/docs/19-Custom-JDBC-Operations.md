# Custom JDBC Operations

Doobie aims to provide a nice API for typical database operations, 
but sometimes you need more - such as calling specific JDBC methods.
In this page we list ways you can customize your JDBC query, from high level to lower level.

## Low-level JDBC API (`doobie.free.*` / `doobie.F*`)

In the low-level API, Doobie encodes the JDBC API as a [free monad](https://typelevel.org/cats/datatypes/freemonad.html) 
(hence `free` in the package name).
Each standard JDBC type has its own free monad counterpart covering its operations

- `doobie.free.ConnectionIO`/`doobie.FC` represents operations over `java.sql.Connection`
- `doobie.free.PreparedStatement`/`doobie.FPS` represents operations over `java.sql.PreparedStatement`
- `doobie.free.ResultSetIO`/`doobie.FRS` represents operations over `java.sql.ResultSet`
- ...and similarly for other JDBC types

For example, in `doobie.FC`/`doobie.free.connection` module you will find equivalents to the methods on `java.sql.Connection` class.
(FC is an acronym where F == Free and C == Connection). Similarly, `doobie.FPS`/`doobie.free.preparedstatement` module contains equivalents to the methods on `java.sql.PreparedStatement` class.

Just like we need to be careful to close Connections and PreparedStatements with JDBC, **care must be taken when using
doobie's low-level API to ensure resources like Connections and PreparedStatements are closed.**

### Useful low-level operations

#### `raw`: Access to the underlying JDBC object

Provides access to the underlying JDBC object.

```scala mdoc
import doobie.FRS // alias for doobie.free.resultset

FRS.raw { resultSet: java.sql.ResultSet =>
  // ... do something with the ResultSet
  resultSet.getInt(1)
}

```

#### `embed`: Convert between various `*IO`s

The `embed` method allows you to compose a "lower-level" JDBC operation
chain into a "higher-level" one.

In the example below, we embed a `ResultSetIO` into a `PreparedStatementIO`, which is itself then embedded into a `ConnectionIO`

```scala mdoc:silent
import doobie.{FPS, FC}
import doobie.{ResultSetIO, ConnectionIO}
import cats.effect.syntax.all._
import doobie.syntax.all._

val readFirstRowCol1: ResultSetIO[String] = FRS.next.flatMap(_ => FRS.getString(1))

// Use bracket to ensure resources are closed
val embedExample: ConnectionIO[String] = FC.prepareStatement("SELECT 1").bracket { preparedStatement =>
  FC.embed(
    preparedStatement,
    FPS.executeQuery.bracket { resultSet =>
      FPS.embed(resultSet, readFirstRowCol1)
    }(resultSet => FPS.embed(resultSet, FRS.close)))
}(ps => FC.embed(ps, FPS.close))
```

#### Other operations

You will find many familiar methods from `cats.effect.IO`
with the same semantics, such as:

- `pure`: Lift a pure value
- `raiseError`: Raise an error
- `delay`: Suspend a computation

These are eventually interpreted into equivalent `cats.effect.IO` operations.

## High-level API (`doobie.hi.*` / `doobie.H*`)

The `doobie.hi.*` modules provide high-level APIs which handle concerns like
closing Connections and logging for you. The high-level module builds upont the low-level API (`doobie.free.*`).

In the example below, we use `doobie.HC.executeWithResultSet`
to execute a query and obtain the results. There is no need explicitly close the Connection, PreparedStatement or ResultSet
because it's handled by `HC.executeWithResultSet` already.

```scala mdoc:silent
import cats.effect.IO
import cats.effect.unsafe.implicits.global // To allow .unsafeRunSync
import doobie.Transactor

// Create the transactor
val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
  driver = "org.postgresql.Driver", 
  url = "jdbc:postgresql:world",   
  user = "postgres",
  password = "password",
  logHandler = None                  
)
```

```scala mdoc:silent
import doobie.HC  // High-level API over java.sql.Connection
import doobie.HRS  // High-level API over java.sql.ResultSet
import doobie.ConnectionIO
import doobie.util.log.{LoggingInfo, Parameters}
import cats.effect.unsafe.implicits.global

val sql = "SELECT * FROM (VALUES (1, '1'), (2, '2'))"
val program: ConnectionIO[List[(Int, String)]] = HC.executeWithResultSet(
  create = FC.prepareStatement(sql),
  prep = FPS.unit,
  exec = FPS.executeQuery,
  process = HRS.list[(Int, String)],
  loggingInfo = LoggingInfo(sql, Parameters.NonBatch(List.empty), label = doobie.util.unlabeled)
)
```

```scala mdoc
program.transact(xa).unsafeRunSync()
```

### Useful high-level APIs in `doobie.hi.connection`/`doobie.HC`

- `executeWithResultSet`: Create and execute a `PreparedStatement` and then process the ResultSet
- `executeWithoutResultSet`: Create and execute a `PreparedStatement` which immediately returns the result without reading from a `ResultSet`
  (e.g. for updates since the updated row count does not require reading from a `ResultSet`)
- `stream`: Execute a `PreparedStatement` query and provide rows in chunks, streamed via `fs2.Stream`

## Tweaking `Query`/`Update` execution with `*AlteringExecution` methods

If you just need to do a small "tweak" to your typical `Query`/`Update` execution steps,
you can use methods like `toAlteringExecution`/`toMapAlteringExecution` to customize the steps.

```scala mdoc
import cats.syntax.all._
import doobie.hi.connection.PreparedExecution

fr"select name from country order by code limit 10"
    .query[String]
    .toAlteringExecution[List] { (steps: PreparedExecution[List[String]]) =>
      steps.copy(
        process = FRS.setFetchSize(5) *> steps.process
      )
    }
    .transact(xa)
    .unsafeRunSync()

```

