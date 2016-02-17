---
layout: book
number: 3
title: Connecting to a Database
---

Alright, let's get going.

In this chapter we start from the beginning. First we write a program that connects to a database and returns a value, and then we run that program in the REPL. We also touch on composing small programs to construct larger ones.

### Our First Program

Before we can use **doobie** we need to import some symbols. We will use the `doobie.imports` module here as a convenience; it exposes the most commonly-used symbols when working with the high-level API. We will also import the [scalaz](https://github.com/scalaz/scalaz) core, as well as `Task` from scalaz-concurrent.

```tut:silent
import doobie.imports._
import scalaz._, Scalaz._
import scalaz.concurrent.Task
```

In the **doobie** high level API the most common types we will deal with have the form `ConnectionIO[A]`, specifying computations that take place in a context where a `java.sql.Connection` is available, ultimately producing a value of type `A`.

So let's start with a `ConnectionIO` program that simply returns a constant.

```tut
val program1 = 42.point[ConnectionIO]
```

This is a perfectly respectable **doobie** program, but we can't run it as-is; we need a `Connection` first. There are several ways to do this, but here let's use a `Transactor`.

```tut:silent
val xa = DriverManagerTransactor[Task](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
```

A `Transactor` is simply a structure that knows how to connect to a database, hand out connections, and clean them up; and with this knowledge it can transform `ConnectionIO ~> Task`, which gives us something we can run. Specifically it gives us a `Task` that, when run, will connect to the database and run our program in a single transaction.

The `DriverManagerTransactor` simply delegates to the `java.sql.DriverManager` to allocate connections, which is fine for development but inefficient for production use. In a later chapter we discuss other approaches for connection management.

Right, so let's do this.

```tut
val task = program1.transact(xa)
task.unsafePerformSync
```

Hooray! We have computed a constant. It's not very interesting because we never ask the database to perform any work, but it's a first step.

> Keep in mind that all the code in this book is pure *except* the calls to `Task.unsafePerformSync`, which is the "end of the world" operation that typically appears only at your application's entry points. In the REPL we use it to force a computation to "happen".

Right. Now let's try something more interesting.

### Our Second Program

Let's use the `sql` string interpolator to construct a query that asks the *database* to compute a constant. We will cover this construction in great detail later on, but the meaning of `program2` is "run the query, interpret the resultset as a stream of `Int` values, and yield its one and only element."

```tut
val program2 = sql"select 42".query[Int].unique
val task2 = program2.transact(xa)
task2.unsafePerformSync
```

Ok! We have now connected to a database to compute a constant. Considerably more impressive. 


### Our Third Program

What if we want to do more than one thing in a transaction? Easy! `ConnectionIO` is a monad, so we can use a `for` comprehension to compose two smaller programs into one larger program.

```tut:silent
val program3 = 
  for {
    a <- sql"select 42".query[Int].unique
    b <- sql"select random()".query[Double].unique
  } yield (a, b)
```

And behold!

```tut
program3.transact(xa).unsafePerformSync
```

The astute among you will note that we don't actually need a monad to do this; an applicative functor is all we need here. So we could also write `program3` as:

```tut:silent
val program3a = {
  val a = sql"select 42".query[Int].unique
  val b = sql"select random()".query[Double].unique
  (a |@| b).tupled
}
```

And lo, it was good:

```tut
program3a.transact(xa).unsafePerformSync
```

And of course this composition can continue indefinitely.

```tut
program3a.replicateM(5).transact(xa).unsafePerformSync.foreach(println)
```


### Diving Deeper

*You do not need to know this, but if you're a scalaz user you might find it helpful.*

All of the **doobie** monads are implemented via `Free` and have no operational semantics; we can only "run" a **doobie** program by transforming `FooIO` (for some carrier type `java.sql.Foo`) to a monad that actually has some meaning. 

Out of the box all of the **doobie** free monads provide a transformation to `Kleisli[M, Foo, ?]` given `Monad[M]`, `Catchable[M]`, and `Capture[M]` (we will discuss `Capture` shortly, standby). The `transK` method gives quick access to this transformation.

```tut
val kleisli = program1.transK[Task] 
val task = Task.delay(null: java.sql.Connection) >>= kleisli
task.unsafePerformSync // sneaky; program1 never looks at the connection
```

So the `Transactor` above simply knows how to construct a `Task[Connection]`, which it can bind through the `Kleisli`, yielding our `Task[Int]`. There is a bit more going on (we add commit/rollback handling and ensure that the connection is closed in all cases) but fundamentally it's just a natural transformation and a bind.

In addition to the `transK` syntax above, **doobie** provides natural transformations on each algebra's module. For example `doobie.free.connection` (aliased as `FC` in `doobie.imports`) provides:

- `trans[M: Monad: Capture: Catchable](c: Connection): ConnectionIO ~> M`
- `transK[M: Monad: Capture: Catchable]: ConnectionIO ~> Kleisli[M, Connection, ?]`

And analogously for other modules in `doobie.free`.


#### The Capture Typeclass

Currently scalaz has no typeclass for monads with **effect-capturing unit**, so that's all `Capture` does; it's simply `(=> A) => M[A]` that is referentially transparent for *all* expressions, even those with side-effects. This allows us to sequence the same effect multiple times in the same program. This is exactly the behavior you expect from `IO` for example. 

**doobie** provides instances for `Task` and `IO`, and the implementations are simply `delay` and `apply`, respectively.

> Note that `scala.concurrent.Future` does **not** have an effect-capturing constructor and thus cannot be used as a target type for **doobie** programs. Although `Future` is very commonly used for side-effecting operations, doing so is not referentially transparent. *`Future` has nothing at all to say about side-effects. It is well-behaved in a functional sense only for pure computations.*

