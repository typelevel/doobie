# Doobie

> **Hello tweeters** ... it seems that the v0.1 announcement from July was picked up in a tweetstorm on Nov 22. So just an update, v0.2 is well on its way and will be out by year's end*, so check back for some really nice new stuff coming up soon. - rob
>
> *Soon thereafter, actually. Writing doc made me realize I needed a few more features.

This is a pure functional JDBC layer for Scala. It is not an ORM, nor is it a relational algebra; it just provides a principled way to construct programs (and higher-level APIs) that use JDBC. Doobie introduces very few new abstractions; if you are familiar with basic `scalaz` typeclasses like `Functor` and `Monad` you should have no trouble here. The obligatory code sample (without imports, to keep you guessing):

```scala
// Transactor just closes over connection info and knows how to allocate connections
scala> val xa = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
xa: doobie.util.transactor.Transactor[scalaz.concurrent.Task] = doobie.util.transactor$DriverManagerTransactor$$anon$1@c7efaf9

// An action to load up some test data
scala> val load = sql"RUNSCRIPT FROM 'world.sql' CHARSET 'UTF-8'".executeUpdate.void
load: doobie.hi.ConnectionIO[Unit] = Gosub()

// A case class to read
scala> case class Country(name: String, continent: String, population: Int)
defined class Country

// A parameterized query, as a scalaz-stream Process
scala> def q(e: Double) = sql"SELECT NAME, CONTINENT, POPULATION FROM COUNTRY WHERE LIFEEXPECTANCY >= $e".process[Country]
q: (e: Double)scalaz.stream.Process[doobie.hi.ConnectionIO,Country]

// Our program is two transactions, one loading data and one querying and streaming results to stdout
scala> val program = xa.transact(load) *> xa.transact(q(80)).take(5).map(_.toString).to(io.stdOutLines).run
program: scalaz.concurrent.Task[Unit] = scalaz.concurrent.Task@51f0819c

// Run it!
scala> program.run
Country(Andorra,Europe,78000)
Country(Japan,Asia,126714000)
Country(Macao,Asia,473000)
Country(San Marino,Europe,27000)
Country(Singapore,Asia,3567000)

scala> // yay!
```

## Quick Start

The current release is **0.1** which is a **preview release** intended for scalaz users who are interested in playing with the API. You should expect breaking changes for at least the next few versions. To use Doobie you need to add the following to your `build.sbt`.

```scala
resolvers ++= Seq(
  "tpolecat" at "http://dl.bintray.com/tpolecat/maven",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)

libraryDependencies += "org.tpolecat" %% "doobie-core" % "0.1"
```

Doobie **0.1** works with **Scala 2.10** and **scalaz 7.1**; the **tip** supports **Scala 2.11**, as will the next release of doobie. So `publish-local` or stay tuned for a release.

Right now documentation is a bit sparse, but if you look at the high level and low level examples (links below, to encourage you to read the design notes) and check out the scaladoc (ditto) and maybe look at, you know, the source, you should be able to follow the types and hack something up for yourself. And of course you can always find me on Twitter and on the `#scala` and `#scalaz` IRC channels on FreeNode.

If you try out Doobie **please** let me know how it goes. If it sucks, file an issue and tell me why. 


## Design Notes

#### General Design

Doobie is a library in the spirit of those on [typelevel.org](http://typelevel.org) and as such it follows some rules that we can state up front (and argue about, as needed).

- Doobie is provided as modules of types and functions; in order to use it you simply import symbols. It's all *a-la-carte*; there is no Sizzler import. In the end I think this makes things simpler because it's always very clear where things originate.
- Doobie adheres to the scalazzi safe subset (the types given can be trusted) and the tpolecat bad attitude subset (no macros).
- Because JDBC is a heavily side-effecting API with many failure modes (most of which are fatal) Doobie provides exception-handling combinators a la `MonadCatchIO` rather than computing all values in a disjunction. I have tried it both ways and this is what I prefer. You are free to lift any subprogram into `Throwable \/ A` via `.attempt` if you wish, or write a new interpreter that carries exceptions along rather than allowing them to propagate.
- I think documentation is important, so I have put some effort into making Doobie produce useful Scaladoc. It's still sparse but it's easy to peruse (in part due to the simple module structure). [Check it out](http://tpolecat.github.io/doc/doobie/0.1/api/index.html).

#### Low-Level API

The low-level API provided by `doobie.free` is a family of machine-generated algebras for each of the "main" JDBC types (`Connection`, `ResultSet`, etc.) and free monads thereof. This API is intended for library developers or users who need to step out of the safer `hi` API described below. The low-level API has the following properties:

- Each method in the Java API has an associated smart constructor in the low-level API. The names, types, and computed result types are identical to those in the Java API, and the implementation in the provided Kleisli interpreter (described below) is a direct delegation. This 1:1 correspondence makes it  straightforward to construct pure monadic equivalents of existing imperative JDBC code.
- In addition to constructors for operations on carrier types, Doobie also provides an effect-capturing constructor (essential when working with native JDBC objects) and an exception-catching constructor in each algebra, as well as lifting constructors to embed programs written in the free monads of other algebras.
- For each algebra we provide a natural transformation to `Kleisli[M, C, A]` given carrier type `C`, target monad `M`, and evidence that `M` can capture effects and catch exceptions. Or you can of course write your own interpreter. Note that the provided natural transformation for `DriverManager` (which has no carrier type) goes straight to `M[A]`.

Because the constructors are in a 1:1 correspondence with JDBC (with no swizzling of types whatsoever) this API must be used with a great deal of caution; many computed types have side-effecting methods and some are liftetime-managed and must be closed in a particular order.

An example program written with this API: for **v0.1** [here](../v0.1/example/src/main/scala/example/FreeUsage.scala) • for the **tip** [here](example/src/main/scala/example/FreeUsage.scala).

#### High-Level API

The high-level API provided by `doobie.hi` directly exposes a subset of constructors provided by `doobie.free` and adds new constructors with the following characteristics:

- JDBC integer flags are swizzled to/from proper enumerated types defined in `doobie.enum`.
- Actions that compute lifetime-managed types do not expose the computed value directly, but rather take a continuation written in that carrier type's monad. These actions guarantee resource safety.
- For setting query parameters and getting/updating column results the high-level API provides typeclass-driven `get`, `update`, and `set` constructors that can read, update, and write primitive values, as well as [nested] products and case classes thereof (these may span multiple columns). This also sweeps up the issue of `null` values, which must be handled via `Option`; reading a `null` column value into a non-option type is an error, for example.
- Constructors are provided that allow `ResultSet` programs to be treated as `scalaz.stream.Process` values, which provides a natural way to do many kinds of common tasks such as transforming and streaming results to an output channel.

Note that the types in `doobie.hi` and `doobie.free` are identical; you can freely mix code written using either API, and indeed you will need to if you're dealing with vendor-specific extensions or types like `CLOB` that are not supported directly in the high-level API.

Some example programs written with this API: for **v0.1** [here](../v0.1/example/src/main/scala/example/HiUsage.scala) • for the **tip** [here](example/src/main/scala/example/HiUsage.scala) and [here](example/src/main/scala/example/FirstExample.scala) and probably some others in that directory.

#### Other Facilities

Doobie provides, in no particular order, some other stuff:

- Additional combinators for `Process` to make sinking and folding operations somewhat easier.
- Additional combinators for `Catchable` that bring it into parity with `MonadCatchIO`. 
- Bedazzlement for the above.
- A `sql` string interpolater for typesafe statement construction.

## Anticipated FAQs

- **Why do I have to write my own goddamn SQL?** Abstracting away the database (and vendor) is a very hard general problem, and in my experience every solution is lacking; I always end up dropping down to native SQL very quickly. So I didn't try to solve this problem. However, Doobie provides a basis upon which such a solution could be built.
- **What's up with the lawless `Capture` typeclass?** scalaz has been a little handwavey with calling conventions for monadic unit and I need a way to say what I mean, so that's what `Capture` does. When we lift a value into a monadic context we can do so by value (i.e., eagerly) or by need (i.e., lazily) or by name (i.e., thunkily). `Task` and `IO` variously provide all three, but the third is the one I'm after. The **effect-capturing unit** is the FFI that allows us to turn an effect into a value, and this is what `Capture` is intended to do. So `Capture[IO]` is just `IO.apply` and `Capture[Task]` is `Task.delay` or `Task.apply` depending on whether you wish to [logically] fork or not.
- **Can we make imports more like scalaz so it's less fiddly?** Maybe. It's simple but irritating right now, which may or may not be better than complex and non-irritating. Let me know what you think.

