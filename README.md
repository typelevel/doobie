## doobie - mellow database access

**Status**: still pre-0.1 but I'm open to comments at this point.

This is a pure functional JDBC layer for Scala. It is not an ORM, nor is it a relational algebra; it  provides a principled way to construct programs (and higher-level APIs) that use JDBC. Doobie introduces very few new abstractions; if you are familiar with basic `scalaz` typeclasses like `Functor` and `Monad` you should have no trouble here.

#### General Design

Doobie is a principled library in the spirit of those on [typelevel.org](http://typelevel.org) and as such it follows some rules that we can state up front (and argue about, as needed).

- Doobie is provided as modules of types and functions; in order to use it you simply import symbols. No subclassing.
- Doobie adheres to the scalazzi safe subset (the types given can be trusted) and the tpolecat bad attitude subset (no macros).
- Because JDBC is a heavily side-effecting API with many failure modes (most of which are fatal) Doobie provides exception-handling combinators a la `MonadCatchIO` rather than computing all values in a disjunction. I have tried it both ways and this is what I prefer. You are free to lift any subprogram into `Throwable \/ A` via `.attempt` if you wish, or write a new interpreter that carries exceptions along rather than allowing them to propagate.
- I think documentation is important, so I have put some effort into making Doobie produce useful Scaladoc. It's still very sparse but it's very easy to peruse (in large part due to the simple module structure).

#### Low-Level API

The low-level API provided by `doobie.free` is a family of machine-generated algebras for each of the "main" JDBC types (`Connection`, `ResultSet`, etc.) and free monads thereof. This API is intended for library developers or users who need to step out of the safer `hi` API described below. The low-level API has the following properties:

- Each method in the Java API has an associated smart constructor in the low-level API. The names, types, and computed result types are identical to those in the Java API, and the implementation in the provided Kleisli interpreter (described below) is a direct delegation. This 1:1 correspondence makes it  straightforward to construct pure monadic equivalents of existing imperative JDBC code.
- In addition to constructors for operations on carrier types, Doobie also provides an effect-capturing constructor (essential when working with native JDBC objects) and an exception-catching constructor in each algebra, as well as lifting constructors to embed programs written in the free monads of other algebras.
- For each algebra we provide a natural transformation to `Kleisli[M, C, A]` given carrier type `C`, target monad `M`, and evidence that `M` can capture effects and catch exceptions. Or you can of course write your own interpreter. An exception here is the provided natural transformation for `DriverManager`, which has no carrier type and goes straight to `M[A]`.

Because the constructors are in a 1:1 correspondence with JDBC (with no swizzling of types whatsoever) this API must be used with a great deal of caution; many computed types have side-effecting methods and some are liftetime-managed and must be closed in a particular order.

An example program written with this API is provided [here](example/src/main/scala/example/FreeUsage.scala).

#### High-Level API

The high-level API provided by `doobie.hi` directly exposes a subset of constructors provided by `doobie.free` and adds new constructors with the following characteristics:

- JDBC integer flags are swizzled to/from proper enumerated types defined in `doobie.enum`.
- Actions that compute lifetime-managed types do not expose the computed value directly, but rather take a continuation written in that carrier type's monad. These actions guarantee resource safety.
- For setting query parameters and getting column results the high-level API provides typeclass-driven `get` and `set` constructors that can read and write primitive values, as well as [nested] products and case classes thereof (these may span multiple columns). This also sweeps up the issue of `null` values, which must be handled via `Option`; reading a `null` value into a non-option type is an error.
- Constructors are provided that allow `ResultSet` programs to be treated as `scalaz.stream.Process` values, which provides a natural way to do many kinds of common tasks such as transforming and streaming results to an output channel.

Note that the types in `doobie.hi` and `doobie.free` are identical; you can freely mix code written using either API, and indeed you will need to if you're dealing with vendor-specific extensions or types like `CLOB` that are not supported directly in the high-level API.

An example program written with this API is provided [here](example/src/main/scala/example/HiUsage.scala).

#### Other Facilities

Doobie provides, in no particular order, some other stuff:

- Additional combinators for `Process` to make sinking and folding operations somewhat easier.
- Additional combinators for `Catchable` that bring it into parity with `MonadCatchIO`. 
- Syntax for the above.
- A `sql` string interpolater for typesafe statement construction.

#### Left to Do

In no particular order:

- `Catchable` combinators that are specific to JDBC, as well as SQLSTATE tables for common vendors.
- Structured logging. This will need to get wired through the `Kleieli` interpreters.
- Flesh out the `Atom` implementations for all ground types, and add example of using tagged types for alternative mappings.
- String interpolator for callable statements (needs In/Out/InOut params).
- Expand `Atom/Composite` and `hi` to handle resultset updating.
- Alternative instances of `Capture[Task]` that forks native calls (right now everything is straight-line).
- More/bigger examples.

