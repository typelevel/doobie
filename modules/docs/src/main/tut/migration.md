---
layout: page
title:  "migration"
section: "migration"
position: 2
---

# Migrating from 0.4.x with Scalaz

**doobie** has standardized on [cats](https://github.com/typelevel/cats) as its FP support library. If you are using scalaz for other parts of your application you can interpret `ConnectionIO` into `scalaz.concurrent.Task` at the boundary by using a `Transactor[Task]`. The required instance of `Async[Task]` is provided by [fs2-scalaz](https://github.com/functional-streams-for-scala/fs2-scalaz).

# Migrating from 0.4.x with Cats

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

