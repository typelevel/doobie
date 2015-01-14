---
layout: book
number: 8
title: Error Handling
---

### Setting Up

```tut:silent
import scalaz._, Scalaz._, doobie.imports._
```

### About Exceptions

Exceptions are a fact of life when interacting with databases, and they are largely nondeterministic; whether an operation will succeed or not depends on unpredictable factors like network health, the current contents of tables, locking state, and so on. So we must decide whether to compute everything in a disjunction like `EitherT[ConnectionIO, Throwable, A]` or allow exceptions to propagate until they are caught explicitly. **doobie** adopts the second strategy: exceptions are allowed to propagate and escape unless handled explicitly (exactly as `IO` and `Task` work). This means when a **doobie** action (transformed to some target monad) is executed, exceptions can escape.

There are three main types of exceptions that are likely to arise:

1. Various types of `IOException` can happen with any kind of I/O, and these exceptions tend to be unrecoverable.
1. Database exceptions, typically as a generic `SQLException` with a vendor-specific `SQLState` identifying the specific error. Some vendors (PostgreSQL for instance) publish a table of error codes, and in these cases **doobie** can provide a matching set of exception-handling combinators. However in most cases the error codes must be passed down as folklore or discovered by experimentation. There exist the XOPEN and SQL:2003 standards, but it seems that no vendor adheres closely to these specifications. Some of these errors are recoverable and others aren't.
1. **doobie** will raise an `InvariantViolation` in response to invalid type mappings, unknown JDBC constants returned by drivers, observed `NULL` values, and other violations of invariants that **doobie** assumes. These exceptions indicate programmer error or driver non-compliance and are generally nonrecoverable.

### The `Catchable` Typeclass and Derived Combinators

All **doobie** monads have associated instances of the `scalaz.Catchable` typeclass, and the provided interpreter requires all target monads to have an instance as well. `Catchable` provides two operations:

- `attempt` converts `M[A]` into `M[Throwable \/ A]`
- `fail` constructs an `M[A]` that fails with a provided `Throwable`

So any **doobie** program can be lifted into a disjunction simply by adding `.attempt`.

```tut
val p = 42.point[ConnectionIO]
p.attempt
```

From the `.attempt` combinator we derive the following, available as combinators and as syntax:

- `attemptSome` allows you to catch only specified `Throwable`s.
- `except` recovers with a new action.
- `exceptSome` same, but only for specified `Throwable`s.
- `onException` executes an action on failure, discarding its result.
- `ensuring` executes an action in all cases, generalizing `finally`.

From these we can derive combinators that only pay attention to `SQLException`:

- `attemptSql` is like `attempt` but only traps `SQLException`. 
- `attemptSomeSql` traps only specified `SQLException`s.
- `exceptSql` recovers from a `SQLException` with a new action.
- `onSqlException` executes an action on `SQLException` and discards its result.

And finally we have a set of combinators that focus on `SQLState`s.

- `attemptSqlState` is like `attemptSql` but yields `M[SQLState \/ A]`.     
- `attemptSomeSqlState` traps only specified `SQLState`s.
- `exceptSqlState` recovers from a `SQLState` with a new action.
- `exceptSomeSqlState`  recovers from specified `SQLState`s with a new action.

See the ScalaDoc for more information.

