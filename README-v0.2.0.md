# doobie

**doobie** is a pure functional JDBC layer for Scala. It is not an ORM, nor is it a relational algebra; it just provides a principled way to construct programs (and higher-level APIs) that use JDBC. **doobie** introduces very few new abstractions; if you are familiar with basic `scalaz` typeclasses like `Functor` and `Monad` you should have no trouble here.

## Quick Start

The current release is **0.2.0**. You should expect breaking changes for at least the next few versions, although these will be documented and kept to a minimum. To use **doobie** you need to add the following to your `build.sbt`.

```scala
resolvers ++= Seq(
  "tpolecat" at "http://dl.bintray.com/tpolecat/maven",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)

libraryDependencies ++= {
  "org.tpolecat" %% "doobie-core" % "0.2.0", 
  "org.tpolecat" %% "doobie-contrib-postgres" % "0.2.0",      // for PostgreSQL-specific types
  "org.tpolecat" %% "doobie-contrib-h2" % "0.2.0",            // for H2-specific types
  "org.tpolecat" %% "doobie-contrib-specs" % "0.2.0" % "test" // support for query checking in Specs2 
}
```

**doobie** **0.2.0** works with **Scala 2.10 and 2.11** and **scalaz 7.1**.

## Documentation and Support

- Behold the [**book of doobie**](http://tpolecat.github.io/doobie-0.2.0-SNAPSHOT/00-index.html). Start here.
- If you have comments or run into trouble, please file an issue.
- Find **tpolecat** on the FreeNode `#scala` channel.

## Design Notes

#### General Design

**doobie** is a library in the spirit of [scalaz](https://github.com/scalaz/scalaz) and [typelevel](http://typelevel.org) and as such it follows some rules that we can state up front (and argue about, as needed).

- **doobie** is provided as modules of types and functions; in order to use it you simply import symbols. A typical set of imports is available all at once via the `doobie.imports` module.
- **doobie** adheres to the scalazzi safe subset (the types given can be trusted) and the tpolecat bad attitude subset (no macros ... other than typeclass instance deriving courtesy of [shapeless](https://github.com/milessabin/shapeless)).
- Because JDBC is a heavily side-effecting API with many failure modes (most of which are fatal) **doobie** provides exception-handling combinators a la `MonadCatchIO` rather than computing all values in a disjunction. I have tried it both ways and this is what I prefer. You are free to lift any subprogram into `Throwable \/ A` via `.attempt` if you wish, or write a new interpreter that carries exceptions along rather than allowing them to propagate.

#### Low-Level API

The low-level API provided by `doobie.free` is a family of machine-generated algebras for each of the "main" JDBC types (`Connection`, `ResultSet`, etc.) and free monads thereof. This API is intended for library developers or users who need to step out of the safer `hi` API described below. The low-level API has the following properties:

- Each method in the Java API has an associated smart constructor in the low-level API. The names, types, and computed result types are identical to those in the Java API, and the implementation in the provided Kleisli interpreter (described below) is a direct delegation. This 1:1 correspondence makes it  straightforward to construct pure monadic equivalents of existing imperative JDBC code.
- In addition to constructors for operations on carrier types, **doobie** also provides an effect-capturing constructor (essential when working with native JDBC objects) and an exception-catching constructor in each algebra, as well as lifting constructors to embed programs written in the free monads of other algebras.
- For each algebra we provide a natural transformation to `Kleisli[M, C, A]` given carrier type `C`, target monad `M`, and evidence that `M` can capture effects and catch exceptions. Or you can of course write your own interpreter. Note that the provided natural transformation for `DriverManager` (which has no carrier type) goes straight to `M[A]`.

Because the constructors are in a 1:1 correspondence with JDBC (with no swizzling of types whatsoever) this API must be used with a great deal of caution; many computed types have side-effecting methods and some are liftetime-managed and must be closed in a particular order.

An example program written with this API is available [here](example/src/main/scala/example/FreeUsage.scala).

#### High-Level API

The high-level API provided by `doobie.hi` directly exposes a subset of constructors provided by `doobie.free` and adds new constructors with the following characteristics:

- JDBC integer flags are swizzled to/from proper enumerated types defined in `doobie.enum`.
- Actions that compute lifetime-managed types do not expose the computed value directly, but rather take a continuation written in that carrier type's monad. These actions guarantee resource safety.
- For setting query parameters and getting/updating column results the high-level API provides typeclass-driven `get`, `update`, and `set` constructors that can read, update, and write primitive values, as well as [nested] products and case classes thereof (these may span multiple columns). This also sweeps up the issue of `null` values, which must be handled via `Option`; reading a `null` column value into a non-option type is an error, for example.
- Constructors are provided that allow `ResultSet` programs to be treated as `scalaz.stream.Process` values, which provides a natural way to do many kinds of common tasks such as transforming and streaming results to an output channel.

Note that the types in `doobie.hi` and `doobie.free` are identical; you can freely mix code written using either API, and indeed you will need to if you're dealing with vendor-specific extensions or types like `CLOB` that are not supported directly in the high-level API.

Some example programs written with this API [here](example/src/main/scala/example/HiUsage.scala) and [here](example/src/main/scala/example/FirstExample.scala) and others in that directory.

#### Other Facilities

**doobie** provides, in no particular order, some other stuff:

- Additional combinators for `Process` to make sinking and folding operations somewhat easier.
- Additional combinators for `Catchable` that bring it into parity with `MonadCatchIO`. 
- Bedazzlement for the above.
- A `sql` string interpolater for typesafe statement construction.

## Anticipated FAQs

- **Why do I have to write my own SQL?** Abstracting away the database (and vendor) is a very hard general problem, and in my experience every solution is lacking; I always end up dropping down to native SQL very quickly. So I didn't try to solve this problem. However, **doobie** provides a basis upon which such a solution could be built.
- **What's up with the lawless `Capture` typeclass?** scalaz has been a little handwavey with calling conventions for monadic unit and I need a way to say what I mean, so that's what `Capture` does. When we lift a value into a monadic context we can do so by value (i.e., eagerly) or by need (i.e., lazily) or by name (i.e., thunkily). `Task` and `IO` variously provide all three, but the third is the one I'm after. The **effect-capturing unit** is the FFI that allows us to turn an effect into a value, and this is what `Capture` is intended to do. So `Capture[IO]` is just `IO.apply` and `Capture[Task]` is `Task.delay` or `Task.apply` depending on whether you wish to [logically] fork or not.

