---
layout: docs
number: 16
title: Extensions for H2
---

## {{page.title}}

In this chapter we discuss the extended support that **doobie** offers for users of [H2](http://www.h2database.com/html/main.html) . To use these extensions you must add an additional dependency to your project:

```scala
libraryDependencies += "org.tpolecat" %% "doobie-h2" % "{{site.doobieVersion}}"
```

This library pulls in H2 as a transitive dependency.

### Array Types

**doobie** supports H2 arrays of the following types:

- `Boolean`
- `Int`
- `Long`
- `Float`
- `Double`
- `String`

In addition to `Array` you can also map to `List` and `Vector`.

See the previous chapter on **SQL Arrays** for usage examples.

### Other Nonstandard Types

- The `uuid` type is supported and maps to `java.util.UUID`.

### H2 Connection Pool

**doobie** provides a `Transactor` that wraps the connection pool provided by H2. Because the transactor has internal state, constructing one is a side-effect that must be captured (here by `IO`).

```scala mdoc:silent:reset
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.h2._

object H2App extends IOApp {

  // Resource yielding a transactor configured with a bounded connect EC and an unbounded
  // transaction EC. Everything will be closed and shut down cleanly after use.
  val transactor: Resource[IO, H2Transactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      be <- Blocker[IO]    // our blocking EC
      xa <- H2Transactor.newH2Transactor[IO](
              "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", // connect URL
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
