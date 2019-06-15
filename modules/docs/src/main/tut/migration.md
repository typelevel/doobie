---
layout: page
title:  "migration"
section: "migration"
position: 2
---

# Upgrading to 0.8.x from 0.7.x

Creating a `Transactor` now requires that the `transactEC` parameter is a `cats.effect.Blocker` instead of an `ExecutionContext`.


-----

# Upgrading to 0.7.x from 0.6.x

`Transactor.connect` has changed from `A => Connection` to `A => Resource[M, Connection]`. Any connection retrieved from this function is expected to close itself.

Methods on `Transactor` that turn a program into a `M` now require that `M` have an instance of `Bracket` instead of `Monad`.

`guarantee` and `onError` have been removed from `catchsql`. `guarantee` should be replaced with `cats.effect.Bracket.guarantee` while `onError` should be replaced with `cats.ApplicativeError.onError`. All other methods now only require an `ApplicativeError` instance. As such the `monaderror` syntax has been renamed to `applicativeerror`.


-----

# Upgrading to 0.6.x from 0.5.x

The major changes in 0.6.x are the addition of explicit threading controls for asynchronous operation and the restructuring of `Meta` and `Composite`, as described below. In preparation for upgrading please **fix all deprecation warnings** as all methods deprecated in 0.5.x have been removed.

## Transactors and Threading

Prior to 0.6.x **doobie** had nothing to say about threading; it was up to users to shift interpreted `IO` programs onto dedicated pools if desired. This has changed. We now identify three distinct execution contexts that are relevant to database applications.

1. All *non-blocking* work is performed on the execution context identified by [`ContextShift[F]`](https://typelevel.org/cats-effect/datatypes/contextshift.html). If you are using [`IOApp`](https://typelevel.org/cats-effect/datatypes/ioapp.html) (which you should) this instance is provided for you. All interpreters (and thus all transactors) need this instance on construction.
1. Requesting a JDBC connection is a blocking operation, so to avoid deadlock these requests cannot be placed in competition with running programs (which need to make progress in order to finish up and return their connections to the pool). Therefore we need a distinct, bounded, blocking execution context for the single purpose of awaiting database connections. All transactors that use a connection pool will require this execution context to be specified.
1. All JDBC primitive operations are [potentially] blocking, so we need a distinct, typically unbounded (since the connection context above provides a logical bound) execution context for scheduling these operations. All transactors that use a connection pool will require this execution context to be specified.

For convenience we provide an `ExecutionContexts` module that provides [`Resource`]()s yielding the kinds of `ExecutionContext`s that will tend to be useful for the scenarios above. Note that `DriverManagerTransactor` provides an unbounded number of connections and is unsuitable for production use anyway, so we do not require the blocking pools here (it uses an unbounded pool internally). Fine for testing but don't use it in real life.

See the book chapter on **Managing Connections** for more information and examples.

#### Streams and Resources

In 0.5.x we provided some single-element `Stream`s that would emit values and guarantee resource cleanup. This has been generalized as `Resource` in cats-effect 1.x, and all such constructors (`H2Transactor.newH2Transactor` for example) now use `Resource` rather than `Stream`. You can use `Stream.resource` to regain the old functionality if desired.

## Meta and Composite

Prior to the 0.6.x series we provided two typeclasses for **bidirectional** type mapping:
- `Meta` defined nullable mappings between column/parameter values and scala types.
- `Composite` defined null-safe mappings betwen column/parameter **vectors** and scala types.

Starting with version 0.6.0 type mappings are **unidirectional**:
- `Meta` has been split into `Get` and `Put` typeclasses, for reads and writes of column/parameter values, respectively.
- `Composite` has been split into `Read` and `Write` typeclasses, for reads and writes of column/parameter vectors, respecitively.

Note that `Meta` does still exist, but only as a mechanism for introducing `Get/Put` pairs. An implicit `Meta[A]` induces both an implicit `Get[A]` and an implicit `Put[A]`, and the old mechanism of `Meta[A].imap(...)(...)` is still supported for this purpose. The `xmap` method has been replaced with parametric `imap` and `TypeTag`-constrained `timap`. Prefer `timap` when possible because it yields better diagnostic information when typechecking queries. `Meta[A]#(t)imap` is the combinatioon of `Get[A]#(t)map` and `Put[A]#(t)contramap`.

To summarize:

| 0.5.x                | 0.6.x                    | Notes |
|----------------------|--------------------------------|--|
| `[A: Meta]`      | `[A: Get : Put]`      | Or just one, depending on usage. |
| `[A: Composite]` | `[A: Read : Write]`   | Or just one, depending on usage. |
| `Meta[A].xmap(..)` | `Meta[A].timap(...)` | Or `imap` when a `TypeTag` is unavailable. |
| `Composite[A].xmap(...)` | `Read[A].map(...)` <br> `Write[A].contramap(...)` | This takes two steps now. |

Please refer to the `examples` project for example usage, or ask questions on the Gitter channel if you have questions or concerns about this change.



-----

# Upgrading to 0.5.x from 0.4.x with Scalaz

**doobie** has standardized on [cats](https://github.com/typelevel/cats) as its FP support library. If you are using scalaz for other parts of your application you can interpret `ConnectionIO` into `scalaz.concurrent.Task` at the boundary by using a `Transactor[Task]`. The required instance of `Async[Task]` is provided by [fs2-scalaz](https://github.com/functional-streams-for-scala/fs2-scalaz).

# Upgrading to 0.5.x from 0.4.x with Cats

This guide covers the changes that are specific to **doobie**, but you will also need to deal with changes between cats 0.9 and cats 1.0 (the provided Scalafix rules may be helpful) and between fs2 0.9 and fs2 0.10. Please join the [gitter channel](https://gitter.im/tpolecat/doobie) if you run into trouble. We will update this document to reflect common issues as they arise.

## Artifacts

Artifact names have changed. The `-cats` segment is now redundant and has been removed.

| 0.4.x Artifact          | 0.5.x Artifact     |
|-------------------------|--------------------|
| `"doobie-core-cats"`      | `"doobie-core"`      |
| `"doobie-h2-cats"`        | `"doobie-h2"`        |
| `"doobie-hikari-cats"`    | `"doobie-hikari"`    |
| `"doobie-postgres-cats"`  | `"doobie-postgres"`  |
| `"doobie-refined-cats"`   | `"doobie-refined"`   |
| `"doobie-scalatest-cats"` | `"doobie-scalatest"` |
| `"doobie-specs2-cats"`    | `"doobie-specs2"`    |


## Imports

Doobie imports have been split into normal and implicit slices, to mirror the pattern in cats (old imports will work but are deprecated) and a few things have been renamed. fs2's interop layer for cats is gone.

| 0.4.x Import         | 0.5.x Import                   |
|----------------------|--------------------------------|
| `doobie.imports._`   | `doobie._` <br> `doobie.implicits._` |
| `doobie.postgres.imports._`   | `doobie.postgres._` <br> `doobie.postgres.implicits._` |
| and so on
| `doobie.util.process` | `doobie.util.stream`          |
| `fs2.interop.cats._` | n/a                            |


## Data Types

The IO data types provided by fs2 and doobie have been subsumed by `cats.effect.IO` or any data type with a `cats.effect.Async` instance, such as `monix.eval.Task`.

| 0.4.x Data Type      | 0.5.x Data Type  |
|----------------------|------------------|
| `fs2.Task`           | `cats.effect.IO` |
| `doobie.util.IOLite` | `cats.effect.IO` |

Note that the "end of the world" operation for `IO` is called `unsafeRunSync`.

## Typeclasses

The typeclass hierarchy in fs2 has been removed.

| 0.4.x  Typeclass       | 0.5.x Typeclass                    |
|------------------------|------------------------------------|
| `fs2.util.Catchable`   | `cats.MonadError[?[_], Throwable]` |
| `fs2.util.Suspendable` | `cats.effect.Sync`                 |

## Transactors

The built-in `Transactor` constructors are now on `Transactor` itself.

| 0.4.x  Constructor             | 0.5.x Constructor                   |
|--------------------------------|-------------------------------------|
| `DriverManagerTransactor(...)` | `Transactor.fromDriverManager(...)` |
| `DataSourceTransactor(...)`    | `Transactor.fromDataSource(...)`   |

## Error-Handling Combinators

**doobie**'s provided `ensuring` combinator has been renamed to `guarantee` to avoid conflicts with Scala's standard library. The combinators for `Catchable` have been removed; use `ApplicativeError` and `MonadError` instead.

## Miscellaneous Changes

In no particular order:

- All `.process` combinators and constructors are deprecated in the `Query/Update` API and removed in the lower-level APIs; use the `.stream` equivalents.
- The `.list` and `.vector` methods on `Query/Query0` are deprecated in favor of `.to[List]` and `.to[Vector]` respectively.

