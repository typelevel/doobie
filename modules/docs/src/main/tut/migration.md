---
layout: page
title:  "migration"
section: "migration"
position: 2
---

# Migrating from 0.4.x with Scalaz

**doobie** has standardized on [cats](https://github.com/typelevel/cats) as its FP support library. If you are using scalaz and do not plan on moving the rest of your codebase to cats you can interpret into `scalaz.concurrent.Task` at the boundary by using a `Transactor[Task]`. You will need to supply an implicit `cats.effect.Async[Task]` instance.

# Migrating from 0.4.x with Cats

This guide covers the changes that are specific to **doobie**, but you will also need to deal with changes between cats 0.9 and cats 1.0 (the provided Scalafix rules may be helpful) and between fs2 0.9 and fs2 0.10. Please join the [gitter channel](https://gitter.im/tpolecat/doobie) if you run into trouble. We will update this document to reflect common issues as they arise.

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
| `monix.eval.Task`    | `cats.effect.IO` |


## Typeclasses

The typeclass hierarchy in fs2 has been removed.

| 0.4.x  Typeclass       | 0.5.x Typeclass                    |
|------------------------|------------------------------------|
| `fs2.util.Catchable`   | `cats.MonadError[?[_], Throwable]` |
| `fs2.util.Suspendable` | `cats.effect.Sync`                 |

## Error-Handling Combinators

**doobie**'s provided `ensuring` combinator has been renamed to `guarantee` to avoid conflicts with Scala's standard library. The combinators for `Catchable` have been removed; use `ApplicativeError` and `MonadError` instead.

