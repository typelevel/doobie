---
layout: docs
number: 9
title: Error Handling
---

## {{page.title}}

In this chapter we examine a set of combinators that allow us to construct programs that trap and handle exceptions.

### Setting Up

```tut:silent
import doobie._
import doobie.implicits._
import cats._
import cats.data._
import cats.effect.IO
import cats.implicits._

// We need a ContextShift[IO] before we can construct a Transactor[IO].
// Note that you don't have to do this if you use IOApp because it's provided for you.
implicit val cs = IO.contextShift(scala.concurrent.ExecutionContext.global)

val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)

val y = xa.yolo
import y._
```

### About Exceptions

Exceptions are a fact of life when interacting with databases, and they are largely nondeterministic; whether an operation will succeed or not depends on unpredictable factors like network health, the current contents of tables, locking state, and so on. So we must decide whether to compute everything in a disjunction like `EitherT[ConnectionIO, Throwable, A]` or allow exceptions to propagate until they are caught explicitly. **doobie** adopts the second strategy: exceptions are allowed to propagate and escape unless handled explicitly (exactly as `IO` works). This means when a **doobie** action (transformed to some target monad) is executed, exceptions can escape.

There are three main types of exceptions that are likely to arise:

1. Various types of `IOException` can happen with any kind of I/O, and these exceptions tend to be unrecoverable.
1. Database exceptions, typically as a generic `SQLException` with a vendor-specific `SQLState` identifying the specific error, are raised for common situations such as key violations. Some vendors (PostgreSQL for instance) publish a table of error codes, and in these cases **doobie** can provide a matching set of exception-handling combinators. However in most cases the error codes must be passed down as folklore or discovered by experimentation. There exist the XOPEN and SQL:2003 standards, but it seems that no vendor adheres closely to these specifications. Some of these errors are recoverable and others aren't.
1. **doobie** will raise an `InvariantViolation` in response to invalid type mappings, unknown JDBC constants returned by drivers, observed `NULL` values, and other violations of invariants that **doobie** assumes. These exceptions indicate programmer error or driver non-compliance and are generally unrecoverable.

### `MonadError` and Derived Combinators

All **doobie** monads provide an `Async` instance, which extends `MonadError[?[_], Throwable]`. This means `ConnectionIO`, etc., have the following primitive operations:

- `.attempt` converts `M[A]` into `M[Either[Throwable, A]]`
- `fail` constructs an `M[A]` that fails with a provided `Throwable`

So any **doobie** program can be lifted into a disjunction simply by adding `.attempt`.

```tut
val p = 42.pure[ConnectionIO]
p.attempt
```

From the `.attempt` and `fail` combinators we can derive many other operations, as described in the Cats documentation. In addition **doobie** provides the following specialized combinators that only pay attention to `SQLException`:

- `attemptSql` is like `attempt` but only traps `SQLException`.
- `attemptSomeSql` traps only specified `SQLException`s.
- `exceptSql` recovers from an `SQLException` with a new action.
- `onSqlException` executes an action on `SQLException` and discards its result.

And finally we have a set of combinators that focus on `SQLState`s.

- `attemptSqlState` is like `attemptSql` but yields `M[Either[SQLState, A]]`.
- `attemptSomeSqlState` traps only specified `SQLState`s.
- `exceptSqlState` recovers from an `SQLState` with a new action.
- `exceptSomeSqlState`  recovers from specified `SQLState`s with a new action.

See the ScalaDoc for more information.

### Example: Unique Constraint Violation

Ok let's set up a `person` table again, using a slightly different formulation just for fun. Note that the `name` column is marked as being unique.

```tut
List(
  sql"""DROP TABLE IF EXISTS person""",
  sql"""CREATE TABLE person (
          id    SERIAL,
          name  VARCHAR NOT NULL UNIQUE
        )"""
).traverse(_.update.quick).void.unsafeRunSync
```

Alright, let's define a `Person` data type and a way to insert instances.

```tut:silent
case class Person(id: Int, name: String)

def insert(s: String): ConnectionIO[Person] = {
  sql"insert into person (name) values ($s)"
    .update
    .withUniqueGeneratedKeys("id", "name")
}
```

The first insert will work.

```tut
insert("bob").quick.unsafeRunSync
```

The second will fail with a unique constraint violation.

```tut
try {
  insert("bob").quick.unsafeRunSync
} catch {
  case e: java.sql.SQLException =>
    println(e.getMessage)
    println(e.getSQLState)
}
```

So let's change our method to return an `Either[String, Person]` by using the `attemptSomeSql` combinator. This allows us to specify the `SQLState` value that we want to trap. In this case the culprit `"23505"` (yes, it's a string) is provided as a constant in the `doobie-postgres` add-on.


```tut:silent
import doobie.postgres._

def safeInsert(s: String): ConnectionIO[Either[String, Person]] =
  insert(s).attemptSomeSqlState {
    case sqlstate.class23.UNIQUE_VIOLATION => "Oops!"
  }
```

Given this definition we can safely attempt to insert duplicate records and get a helpful error message rather than an exception.


```tut
safeInsert("bob").quick.unsafeRunSync

safeInsert("steve").quick.unsafeRunSync
```
