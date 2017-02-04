---
layout: book
number: 10
title: Logging
---

In this chapter we discuss how to log statement execution and timing.

### Setting Up

Once again we will set up our REPL with a transactor.

```tut:silent
import doobie.imports._
#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.data._, cats.implicits._
import fs2.interop.cats._
#-cats

val xa = DriverManagerTransactor[IOLite](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
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

### Basic Statement Logging

When we construct a `Query0` or `Update0` we can provide an optional `LogHandler` that will be given a `LogEvent` on completion and can perform an arbitrary side-effect to report the event as desired.

**doobie** provides an example `LogHandler` that writes a summary to a JDK logger, so let's try that one. Instead of calling `.query` let's call `.queryWithLogHandler`.

```tut:silent
def byName(pat: String) = {
  sql"select name, code from country where name like $pat"
    .queryWithLogHandler[(String, String)](LogHandler.jdkLogHandler)
    .list
    .transact(xa)
}
```

When we run our program we get our result as expected.

```tut
byName("U%").unsafePerformIO
```

But now on standard out we see:

```
Jan 07, 2017 7:07:43 AM doobie.util.log$LogHandler$ $anonfun$jdkLogHandler$1
INFO: Successful Statement Execution:

  select name, code from country where name like ?

 arguments = [U%]
   elapsed = 9 ms exec + 6 ms processing (15 ms total)
```

Let's break down what we're seeing:

- We see the SQL string that is sent to the JDBC driver.
- We see the argument list (in this case just the pattern `U%`).
- We see elapsed time: it took 9ms for the first row to become available, then 6ms to process the rows, for a total of 15ms.

### Implicit Logging

If you wish to turn on logging generally, you can introduce an implicit `LogHandler` that will get picked up and used by the `.query/.update` operations.

```tut:silent
implicit val han = LogHandler.jdkLogHandler

def byName(pat: String) = {
  sql"select name, code from country where name like $pat"
    .query[(String, String)] // handler will be picked up here
    .list
    .transact(xa)
}
```

### Writing Your `LogHandler`

If you use the `jdkLogHandler` you will be warned that it's just an example, write your own! So let's do that. `LogHandler` is a very simple data type:

```scala
case class LogHandler(unsafeRun: LogEvent => Unit)
```

`LogEvent` has three constructors, all of which provide the SQL string and argument list.

- `Success` indicates successful execution and result processing, and provides timing information for both.
- `ExecFailure` indicates that query execution failed, due to a key violation for example. This constructor provides timing information only for the (failed) execution as well as the raised exception.
- `ProcessingFailure` indicates that execution was successful but resultset processing failed. This constructor provides timing information for both execution and (failed) processing, as well as the raised exception.

See the Scaladoc for details on this data type.

The simplest possible `LogHandler` does nothing at all, and this is what you get by default.

```tut:silent
val nop = LogHandler(_ => ())
```

But that's not interesting. Let's at least print the event out.

```tut
val trivial = LogHandler(e => Console.println("*** " + e))
sql"select 42".queryWithLogHandler[Int](trivial).unique.transact(xa).unsafePerformIO
```

The `jdkLogHandler` implementation is straightforward. You might use it as a template to write a logger to suit your particular logging back-end.

```tut:silent
import java.util.logging.Logger
import doobie.util.log._

val jdkLogHandler: LogHandler = {
  val jdkLogger = Logger.getLogger(getClass.getName)
  LogHandler {

    case Success(s, a, e1, e2) =>
      jdkLogger.info(s"""Successful Statement Execution:
        |
        |  ${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
        |
        | arguments = [${a.mkString(", ")}]
        |   elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (${(e1 + e2).toMillis} ms total)
      """.stripMargin)

    case ProcessingFailure(s, a, e1, e2, t) =>
      jdkLogger.severe(s"""Failed Resultset Processing:
        |
        |  ${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
        |
        | arguments = [${a.mkString(", ")}]
        |   elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (failed) (${(e1 + e2).toMillis} ms total)
        |   failure = ${t.getMessage}
      """.stripMargin)

    case ExecFailure(s, a, e1, t) =>
      jdkLogger.severe(s"""Failed Statement Execution:
        |
        |  ${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
        |
        | arguments = [${a.mkString(", ")}]
        |   elapsed = ${e1.toMillis} ms exec (failed)
        |   failure = ${t.getMessage}
      """.stripMargin)

  }
}
```


### Caveats

Logging is not yet supported for streaming (`.process` or YOLO mode's `.quick`).

Note that the `LogHandler` invocation is part of your `ConnectionIO` program, and it is called synchronously. Most back-end loggers are asynchronous so this is unlikely to be an issue, but do take care not to spend too much time in your handler.

Further note that the handler is not transactional; anything your logger does stays done, even if the transaction is rolled back. This is only for diagnostics, not for business logic.
