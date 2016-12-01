---
layout: book
number: 10
title: Logging
---

In this chapter we discuss how to log statement execution and timing.

### Setting Up

Once again we will set up our REPL with a transactor and YOLO mode.

```tut:silent
import doobie.imports._
#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.data._, cats.implicits._
#-cats
val xa = DriverManagerTransactor[IOLite](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
import xa.yolo._
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
Nov 30, 2016 3:45:37 PM doobie.util.log$LogHandler$$anonfun$2 apply
INFO: Successful Statement Execution:

  select name, code from country where name like ?

 arguments = [U%]
   elapsed = 19 ms exec + 9 ms processing (28 ms total)
```

Let's break down what we're seeing:

- We see the SQL string that is sent to the JDBC driver.
- We see the argument list (in this case just the pattern `U%`).
- We see elapsed time: it took 19ms for the first row to become available, then 9ms to process the rows, for a total of 28ms.

### Implicit Logging

If you wish to turn on logging generally for existint code, you can introduce an implicit `LogHandler` that will get picked up and used by the `.query/.update` operations.

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

The simplest possible `LogHandler` does nothing at all, and this is what you get by default.

```tut:silent
val nop = LogHandler(_ => ())
```

But that's not interesting. Let's at least print the event out.

```tut
implicit val trivial = LogHandler(e => Console.println("*** " + e))
sql"select 42".queryWithLogHandler[Int](trivial).unique.transact(xa).unsafePerformIO
```

See the `jdkLogHandler` implementation in `log.scala` in the doobie source code for more information.

### Caveats

Note that the `LogHandler` invocation is part of your program, and it is called synchronously. Most back-end loggers are asynchronous so this is unlikely to be an issue, but do take care not to spend too much time in your handler.

Further note that the handler is not transactional; anything your logger does stays done, even if the transaction is rolled back. This is only for diagnostics, not for business logic.
