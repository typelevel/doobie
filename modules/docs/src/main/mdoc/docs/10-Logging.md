## Logging

In this chapter we discuss how to log statement execution and timing.

### Setting Up

```scala mdoc:silent
import doobie._
import doobie.implicits._
import doobie.util.log.LogEvent
import cats.effect._
import cats.implicits._

// This is just for testing. Consider using cats.effect.IOApp instead of calling
// unsafe methods directly.
import cats.effect.unsafe.implicits.global
```

Logging in doobie is done via a `LogHandlerM` instance that is setup when we create our `Transactor`.

The `LogHandlerM` interface looks like this:

```scala
trait LogHandlerM[M[_]]{
  def run(logEvent: LogEvent): M[Unit]
}
```

The `run` method will be called for all queries executed via the Transactor.
It will receive a LogEvent which it can handle it inside the `M` effect (e.g. `cats.effect.IO`).

For example, let's construct a `LogHandlerM` that only prints out the SQL executed:

```scala mdoc:silent
val printSqlLogHandler: LogHandlerM[IO] = new LogHandlerM[IO] {
  def run(logEvent: LogEvent): IO[Unit] = 
    IO { 
      println(logEvent.sql)
    }
}
```

```scala mdoc:silent
// A transactor that gets connections from java.sql.DriverManager and executes blocking operations
// on an our synchronous EC. See the chapter on connection handling for more info.
val xa = Transactor.fromDriverManager[IO]
  .withLogHandler(printSqlLogHandler)
  .apply(
    "org.postgresql.Driver",     // driver classname
    "jdbc:postgresql:world",     // connect URL (driver-specific)
    "postgres",                  // user
    "password"                   // password
  )
```

We're still playing with the `country` table, shown here for reference.

```sql
CREATE TABLE country (
  code       character(3)  NOT NULL,
  name       text          NOT NULL,
  population integer       NOT NULL,
  gnp        numeric(10,2)
  -- more columns, but we won't use them here
)
```

```scala mdoc:invisible
implicit val mdocColors: doobie.util.Colors = doobie.util.Colors.None
```

```scala mdoc:silent
val pattern = "U%"

sql"select name, code from country where name like $pattern"
    .query[(String, String)]
    .to[List]
    .transact(xa)
    .unsafeRunSync()
```

By now in the standard output above we you can see this line printed out too (from `printSqlLogHandler`)

```
select name, code from country where name like ?
```

### Writing Your Own `LogHandler`

`LogEvent` has three constructors, all of which provide the SQL string and argument list.

- `Success` indicates successful execution and result processing, and provides timing information for both.
- `ExecFailure` indicates that query execution failed, due to a key violation for example. This constructor provides timing information only for the (failed) execution as well as the raised exception.
- `ProcessingFailure` indicates that execution was successful but resultset processing failed. This constructor provides timing information for both execution and (failed) processing, as well as the raised exception.

See the Scaladoc for details on this data type.

Note that you can attach a String `label` when constructing a Query/Update object by calling `.queryWithLabel/.updateWithLabel` instead of `.query/.update`:

```
sql"select name, code from country where name like $pattern"
    .queryWithLabel[(String, String)]("select_country_by_pattern")
    .to[List]
    .transact(xa)
    .unsafeRunSync()
```

You can find the `label` in the LogEvent.

### Passing additional context to your LogHandlerM

You can use cats-effect's `IOLocal` to pass additional context to the LogHandlerM (e.g. tracing context), as the following example shows:

```scala mdoc
import cats.effect.{IOLocal, Ref}
import doobie.util.log.Success

def users = List.range(0, 4).map(n => s"user-$n")

def program: IO[List[String]] =
  for {
    // define an IOLocal where we store the user which caused the query to be run  
    currentUser <- IOLocal("")
    // store all successful sql here, for all users
    successLogsRef <- Ref[IO].of(List.empty[String])
    xa = Transactor.fromDriverManager[IO]
      .withLogHandler(new LogHandlerM[IO] {
        def run(logEvent: LogEvent): IO[Unit] =
          currentUser.get.flatMap(user => successLogsRef.update(logs => s"sql for user $user: '${logEvent.sql}'" :: logs))
      })( // thread through the logHandler here
        "org.h2.Driver",
        "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
        "sa", ""
      )
    // run a bunch of queries
    _ <- users.parTraverse(user =>
      for {
        _ <- currentUser.set(user)
        _ <- sql"select 1".query[Int].unique.transact(xa)
      } yield ()
    )
    // return collected log messages
    logs <- successLogsRef.get
  } yield logs

program.unsafeRunSync().sorted
```

### Caveats

Logging is not yet supported for streaming (`.stream` or YOLO mode's `.quick`).

Note that the `LogHandlerM#run` invocation is part of your `ConnectionIO` program, and it is called synchronously. Most back-end loggers are asynchronous so this is unlikely to be an issue, but do take care not to spend too much time in your handler.

Further note that the handler is not transactional; anything your logger does stays done, even if the transaction is rolled back. This is only for diagnostics, not for business logic.
