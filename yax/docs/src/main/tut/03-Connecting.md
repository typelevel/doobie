---
layout: book
number: 3
title: Connecting to a Database
---

Alright, let's get going.

In this chapter we start from the beginning. First we write a program that connects to a database and returns a value, and then we run that program in the REPL. We also touch on composing small programs to construct larger ones.

### Our First Program

Before we can use **doobie** we need to import some symbols. We will use the `doobie.imports` module here as a convenience; it exposes the most commonly-used symbols when working with the high-level API. We will also import the
#+scalaz
[scalaz](https://github.com/scalaz/scalaz) core, as well as `Task` from scalaz-concurrent.
#-scalaz
#+cats
[cats](https://github.com/typelevel/cats) core and `fs2.interop.cats._`, which provides implicit conversions from fs2 typeclasses to cats typeclasses.
#-cats

```tut:silent
import doobie.imports._
#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.data._, cats.implicits._
import fs2.interop.cats._
#-cats
```

In the **doobie** high level API the most common types we will deal with have the form `ConnectionIO[A]`, specifying computations that take place in a context where a `java.sql.Connection` is available, ultimately producing a value of type `A`.

So let's start with a `ConnectionIO` program that simply returns a constant.

```tut
val program1: ConnectionIO[Int] = 42.pure[ConnectionIO]
```

This is a perfectly respectable **doobie** program, but we can't run it as-is; we need a `Connection` first. There are several ways to do this, but here let's use a `Transactor`.

```tut:silent
val xa = DriverManagerTransactor[IOLite](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
```

A `Transactor` is simply a structure that knows how to connect to a database, hand out connections, and clean them up; and with this knowledge it can transform `ConnectionIO ~> IOLite`, which gives us something we can run. Specifically it gives us an `IOLite` that, when run, will connect to the database and run our program in a single transaction.

Scala does not have a standard IO, so the examples in this book use the simple `IOLite` data type provided by **doobie**. This type is not very feature-rich but is safe and performant and fine to use. Similar monadic types like `scalaz.effect.IO`, `scalaz.concurrent.Task`, `fs2.Task`, and `monix.Task` will also work fine.
#+scalaz
In fact, you can use any Monad `M[_]` as long as there is a `Catchable[M]` and `Capture[M]` available. See *Using Your Own Target Monad* at the end of this capter for more details.
#-scalaz
#+cats
In fact, you can use any Monad `M[_]` as long as there is a `fs2.util.Catchable[M]` and `fs2.util.Suspendable[M]` available. See *Using Your Own Target Monad* at the end of this capter for more details.
#-cats

The `DriverManagerTransactor` simply delegates to the `java.sql.DriverManager` to allocate connections, which is fine for development but inefficient for production use. In a later chapter we discuss other approaches for connection management.

Right, so let's do this.

```tut
val task: IOLite[Int] = program1.transact(xa)
task.unsafePerformIO
```

Hooray! We have computed a constant. It's not very interesting because we never ask the database to perform any work, but it's a first step.

> Keep in mind that all the code in this book is pure *except* the calls to `IOLite.unsafePerformIO`, which is the "end of the world" operation that typically appears only at your application's entry points. In the REPL we use it to force a computation to "happen".

Right. Now let's try something more interesting.

### Our Second Program

Let's use the `sql` string interpolator to construct a query that asks the *database* to compute a constant. We will cover this construction in great detail later on, but the meaning of `program2` is "run the query, interpret the resultset as a stream of `Int` values, and yield its one and only element."

```tut
val program2: ConnectionIO[Int] = sql"select 42".query[Int].unique
val task2: IOLite[Int] = program2.transact(xa)
task2.unsafePerformIO
```

Ok! We have now connected to a database to compute a constant. Considerably more impressive.

### Our Third Program

What if we want to do more than one thing in a transaction? Easy! `ConnectionIO` is a monad, so we can use a `for` comprehension to compose two smaller programs into one larger program.

```tut:silent
val program3: ConnectionIO[(Int, Double)] =
  for {
    a <- sql"select 42".query[Int].unique
    b <- sql"select random()".query[Double].unique
  } yield (a, b)
```

And behold!

```tut
program3.transact(xa).unsafePerformIO
```

The astute among you will note that we don't actually need a monad to do this; an applicative functor is all we need here. So we could also write `program3` as:

```tut:silent
val program3a = {
  val a: ConnectionIO[Int] = sql"select 42".query[Int].unique
  val b: ConnectionIO[Double] = sql"select random()".query[Double].unique
  (a |@| b).tupled
}
```

And lo, it was good:

```tut
program3a.transact(xa).unsafePerformIO
```

And of course this composition can continue indefinitely.

#+scalaz
```tut
program3a.replicateM(5).transact(xa).unsafePerformIO.foreach(println)
```
#-scalaz
#+cats
```tut
val valuesList: ConnectionIO[List[(Int, Double)]] = Applicative[ConnectionIO].replicateA(5, program3a)
val result: IOLite[List[(Int, Double)]] = valuesList.transact(xa)
result.unsafePerformIO.foreach(println)
```
#-cats

### Diving Deeper

*You do not need to know this, but if you're a scalaz user you might find it helpful.*

All of the **doobie** monads are implemented via `Free` and have no operational semantics; we can only "run" a **doobie** program by transforming `FooIO` (for some carrier type `java.sql.Foo`) to a monad that actually has some meaning.

#+scalaz
Out of the box all of the **doobie** provides an interpreter from its free monads to `Kleisli[M, Foo, ?]` given `Monad[M]`, `Catchable[M]`, and `Capture[M]` (we will discuss `Capture` shortly, standby).
#-scalaz
#+cats
Out of the box all of the **doobie** provides an interpreter from its free monads to `Kleisli[M, Foo, ?]` given `Monad[M]`, `fs2.util.Catchable[M]`, and `fs2.util.Suspendable[M]` (we will discuss `Suspendable` shortly, standby).
#-cats

```tut
#+cats
import cats.~>
#-cats
import doobie.free.connection.ConnectionOp
import java.sql.Connection
#+scalaz
import scalaz.~>
#-scalaz

val interpreter: ConnectionOp ~> Kleisli[IOLite, Connection, ?] = KleisliInterpreter[IOLite].ConnectionInterpreter
val kleisli: Kleisli[IOLite, Connection, Int] = program1.foldMap(interpreter)
// >>= is simply flatMap and kleisli.run is (Connection) => IOLite[Int]
val task: IOLite[Int] = IOLite.primitive(null: java.sql.Connection) >>= kleisli.run
task.unsafePerformIO // sneaky; program1 never looks at the connection
```

So the interpreter above is used to transform a `ConnectionIO[A]` program into a `Kleisli[IOLite, Connection, A]`. Then we construct an `IOLite[Connection]` (returning `null`) and bind it through the `Kleisli`, yielding our `IOLite[Int]`. This of course only works because `program1` is a pure value that does not look at the connection.

The `Transactor` that we defined at the beginning of this chapter is basically a utility that allows us to do the same as above using `program1.transact(xa)`.

There is a bit more going on when calling `transact` (we add commit/rollback handling and ensure that the connection is closed in all cases) but fundamentally it's just a natural transformation and a bind.

#+scalaz
#### The Capture Typeclass
#-scalaz
Currently scalaz has no typeclass for monads with **effect-capturing unit**, so that's all `Capture` does; it's simply `(=> A) => M[A]` that is referentially transparent for *all* expressions, even those with side-effects. This allows us to sequence the same effect multiple times in the same program. This is exactly the behavior you expect from `IO` for example.

**doobie** provides `Capture` instances for `Task` and `IO`, and the implementations are simply `delay` and `apply`, respectively.
#+cats
#### The Suspendable Typeclass
`fs2.util.Suspendable` provides a method `delay[A](a: => A): M[A]` that is referentially transparent for *all* expressions, even those with side-effects. This allows us to sequence the same effect multiple times in the same program. This is exactly the behavior you expect from `IO` for example.
#-cats

> Note that `scala.concurrent.Future` does **not** have an effect-capturing constructor and thus cannot be used as a target type for **doobie** programs. Although `Future` is very commonly used for side-effecting operations, doing so is not referentially transparent. *`Future` has nothing at all to say about side-effects. It is well-behaved in a functional sense only for pure computations.*

#### Using Your Own Target Monad
#+cats
As mentioned earlier, you can use any monad `M[_]` when using a `Transactor` as long as there is a `fs2.util.Catchable[M]` and `fs2.util.Suspendable[M]` available. For example, for `monix.eval.Task`, you could have the following:

```tut:silent
import fs2.util.{Catchable, Suspendable}
import monix.eval.Task

implicit object monixTaskCatchableSuspendable extends Catchable[Task] with Suspendable[Task] {
  def pure[A](a: A): Task[A] =
    Task.pure(a)

  def flatMap[A, B](a: Task[A])(f: A => Task[B]): Task[B] =
    a.flatMap(f)

  def fail[A](err: Throwable): Task[A] =
    Task.raiseError(err)

  def attempt[A](fa: Task[A]): Task[Either[Throwable,A]] =
    fa.materialize.map(_.toEither)

  def suspend[A](fa: => Task[A]): Task[A] =
    Task.suspend(fa)
}
```
#-cats
#+scalaz
As mentioned earlier, you can use any monad `M[_]` when using a `Transactor` as long as there is a `scalaz.Catchable[M]` and `doobie.util.capture.Capture[M]` available. For example, for `monix.eval.Task`, you could have the following:
```tut:silent
import doobie.util.capture.Capture
import monix.eval.Task
import scalaz.{Catchable, \/}

implicit object monixTaskCatchable extends Catchable[Task] with Capture[Task] {
  def apply[A](a: => A): Task[A] =
    Task.delay(a)

  def pure[A](a: A): Task[A] =
    Task.pure(a)

  def flatMap[A, B](a: Task[A])(f: A => Task[B]): Task[B] =
    a.flatMap(f)

  def fail[A](err: Throwable): Task[A] =
    Task.raiseError(err)

  def attempt[A](fa: Task[A]): Task[Throwable \/ A] =
    fa.materialize.map(r => \/.fromEither(r.toEither))

  def suspend[A](fa: => Task[A]): Task[A] =
    Task.suspend(fa)
}
```
#-scalaz
