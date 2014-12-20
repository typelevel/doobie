---
layout: book
number: 3
title: Connecting to a Database
---

### Our First Program

First let's get our imports out of the way.

```tut:silent
import doobie.imports._
import scalaz._, Scalaz._, scalaz.concurrent.Task
```

In the **doobie** high level API the most common type you will deal with is `ConnectionIO[A]`, which specifies a computation that takes place in a context where a `java.sql.Connection` is available, and ultimately produces a value of type `A`.

Let's try a very simple computation that simply returns a constant.

```tut
val program = 42.point[ConnectionIO]
```

This is a perfectly cromulent **doobie** program, but we can't run it as-is; we need to know how to connect to a database first. Let's use a `Transactor` to describe how to do this.

```tut:silent
val xa = DriverManagerTransactor[Task](
  "org.h2.Driver",                      // driver class
  "jdbc:h2:mem:ch3;DB_CLOSE_DELAY=-1", // connect URL
  "sa", ""                              // user and pass
)
```

A `Transactor` is simply a structure that knows how to connect to a database, hand out connections, and clean them up; and with this knowledge it can transform `ConnectionIO ~> Task`, which gives us something we can run. Specifically it gives us a `Task` that, when run, will connect to the database and run our program in a single transaction.

```tut
val task = program.transact(xa)
task.run
```

Hooray! We have computed a constant. Now let's try something more interesting.

### Our Second Program

Let's use the `sql` interpolator to construct a *query* that computes a constant. Step by step, right? Note the types here; they are the same as above.

```tut
val program2 = sql"select 42".query[Int].unique
val task2 = program2.transact(xa)
task2.run
```

So we have now connected to a database to compute a constant. Considerably more impressive. 

### Our Third Program

What if we want to do more than one thing in a transaction? Easy! `ConnecionIO` is a monad, so we can [flatmap that shit](http://flatmapthatshit.com/).

```tut:silent
val program3 = 
  for {
    a <- sql"select 42".query[Int].unique
    b <- sql"select sysdate()".query[java.util.Date].unique
  } yield (a, b)
```

And behold!

```tut
program3.transact(xa).run
```


### Diving Deeper

*You do not need to know this, but if you're a scalaz user you might find it helpful.*

All of the **doobie** monads are implemented via `Free` and have no operational semantics; we can only "run" a **doobie** program by transforming `FooIO` (for some carrier type `java.sql.Foo`) to a monad that actually has some meaning. 

Out of the box all of the **doobie** free monads provide a transformation to `Kleisli[M, Foo, A]` given `Monad[M]`, `Catchable[M]`, and `Capture[M]` (we will discuss `Capture` shortly, standby). The `transK` method gives quick access to this transformation.

```tut
val kleisli = program3.transK[Task] 
val task = (null: java.sql.Connection).point[Task] >>= kleisli
```

So the `Transactor` above simply knows how to construct a `Task[Connection]`, which it can bind through the `Kleisli`, yielding our `Task[Int]`.

##### The Capture Typeclass

Currently scalaz has no typeclass for monads with **effect-capturing unit** so that's all `Capture` does; it's simply `(=> A) => M[A]` that is referentially transparent for *all* expressions, even those with side-effects. This allows us to sequence the same effect multiple times in the same program. This is exactly the behavior you expect from `IO` for example. 

**doobie** provides instances for `Task` and `IO`, and the implementations are simply `delay` and `apply`, respectively.



