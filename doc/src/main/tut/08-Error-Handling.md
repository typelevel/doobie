---
layout: book
number: 8
title: Error Handling
---

### Setting Up


### About Exceptions



### Catchable


### CatchSql






---------

### FROM THE WIKI

Doobie provides two strategies for handling exceptions that may be thrown by primitive JDBC operations.

- Each monad in `doobie.free` has a `Catchable` instance, which gives us the `attempt` combinator that takes `M[A]` to `M[Throwable \/ A]`. Rather than lifting all primitive operations into this type and operating in the disjunction (or in `EitherT[M, Throwable, A]`) it is usually simpler to lift an entire subprogram at the point where exceptions can be handled meaningfully.
- Doobie also provides combinators in the spirit of `MonadCatchIO` that allow a more idiomatic style of exception handling, where subprograms can have the equivalent of `catch` and `finally` blocks. These are all defined in terms of `attempt`.

```scala
(setAutoCommit(false) *> doStuff <* commit) onException rollback ensuring close
```


In addition, Doobie provides exception-handling combinators that are specialized to `SQLException` (allowing a subprogram to catch, for instance, only exceptions with a particular `SqlState`).

#### Summary of Combinators

Generic combinators for `Throwable` are defined in `doobie.util.catchable` with syntax provided by  `doobie.syntax.catchable`. Combinators specific to `SQLException` are provided by `catchsql` modules in the same packages. The combinators fall into four categories:

- The `attempt` combinators lift actions into disjunctions and do not install handlers.
- The `except` combinators map exceptions to handler actions that compute a new result.
- The `onException` combinators map exceptions to effectful actions whose results are discarded.
- The `ensuring` combinator generalizes `finally` and attaches an effectful action that is sequenced in all cases.

| `Throwable`  | `SQLException` | `SqlState` |
|--------------|----------------|------------|
| attempt      | attemptSql     | attemptSqlState |
| attemptSome  | attemptSomeSql | attemptSomeSqlState |
| except       | exceptSql      | exceptSqlState |
| exceptSome   |                | exceptSomeSqlState |
| onException  | onSqlException | |
| ensuring     | | |

See the ScalaDoc for more information.

#### `InvariantViolation` Exceptions