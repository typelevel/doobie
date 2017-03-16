# changelog

This file summarizes **notable** changes for each release, but does not describe internal changes unless they are particularly exciting. For complete details please see the corresponding [milestones](https://github.com/tpolecat/doobie/milestones?state=closed) and their associated issues.

----

### <a name="0.4.2"></a>Notable Work in Progress for Version 0.4.2

non-tpolecat contributors thus far: n4to4, Alexa DeWit, wedens, coltfred, Benjamin Trenker

- Replaced all the `doobie.free` internals with a new design that makes it practical to write your own interpreter (or, more commonly, subclass the default one) which is very useful for testing and who knows what else. For most users this will not be an observable change. Book update TBD.
- Switched to a new transactor design that makes it simple to customize behavior, and combined with new interpreter design makes it practical to use **doobie** types in free coproducts (see `coproduct.scala` in the `example` project). This is a **minor breaking change**:
  - The `yolo` member on `Transactor` is no longer stable, so you cannot `import xa.yolo._` anymore; instead you must say `val y = xa.yolo; import y._`. Because this is typically done with `initialCommands` in sbt it's unlikely to be a big deal.
  - `Transactor` is now a final case class with two type parameters which means existing declarations will be wrong. Book update TBD.
- Note that the interpreter/transactor changes require `Monad` instances at a few more call sites, which should be transparent in most cases but may require Cats users to `import fs2.interop.cats._` here and there … if scalac is claiming there's no instance available after upgrading that's probably why.
- Added `list` and `vector` convenience methods to `Query`.
- Updated Posgtgres JDBC driver to v42.0.0.
- Added `set` and `setOpt` combinators for building fragments which contain `SET` operations.
- Updated build to sbt 0.13.13
- Updated to scalaz 7.2.9
- Added support for type refinements (refined library)
- Added a `fail` constructor to all the `F*` modules.
- Made `Meta.nxmap` unnecessary and fixed issues with mappings that are undefined for zero values of underlying unboxed types.

### <a name="0.4.1"></a>New and Noteworthy for Version 0.4.1

This release updates **doobie** to Cats 0.9 and the associated fs2-cats interop layer to 0.3, courtesy of mighty space robot Adelbert Chang. There are no other changes.

### <a name="0.4.0"></a>New and Noteworthy for Version 0.4.0

This was intended to be a quick follow-up to 0.3.0 but it got a little out of hand and turned into a major release. **Please read these notes carefully** because there are some breaking changes relative to 0.3.0.

Eighteen people contributed to this release, of whom seventeen were not tpolecat. They are sparkly and awesome. In no particular order, many many thanks to :sparkles: Channing Walton :sparkles: Daniel Wunsch :sparkles: Gary Coady :sparkles: Tristan Lohman :sparkles: Jisoo Park :sparkles: Yury Liavitski :sparkles: Ikhoon Eom :sparkles: Marek Kadek :sparkles: Leif Wickland :sparkles: Zack Powers :sparkles: Kris Nuttycombe :sparkles: Pepe García :sparkles: Peter Neyens :sparkles: ritschwumm :sparkles: ronanM :sparkles: Sam Ritchie :sparkles: and wedens. :sparkles:

##### Cats Support

This is probably the most important development for 0.4.0 and it is due in large part to the hard work of :sparkles: Jisoo Park :sparkles:. Impossibly huge thanks for his work throughout this process.

<img align="right" height="150" style="padding-left: 20px" src="https://camo.githubusercontent.com/c7a1d594954b34a8277bce52343fe731b14870ad/687474703a2f2f706c61737469632d69646f6c617472792e636f6d2f6572696b2f63617473322e706e67"/>

- **doobie** is now built natively for [Cats](https://github.com/typelevel/cats) with [fs2](https://github.com/functional-streams-for-scala/fs2/tree/series/0.9), with no need for a shim/adapter layer. At the moment this is accomplished via a preprocessor.
- All artifacts are built for Cats. Names are suffixed with `-cats`, so the scalaz version is `doobie-core` and the Cats version is `doobie-core-cats`.
- The **book of doobie** is now published in separate editions for scalaz and Cats.

##### Changes to Core

- There is now support for simple **statement logging** with timing information. This is *not* the long-promised structured logging feature but it fits the common use case and fills a clear functional gap. See the book chapter for details and examples.
- The **dynamic SQL** story is now slightly better with the introduction of **composable statement fragments**. These allow you to build statements from smaller pieces without having to track parameter placeholders/offsets by hand. See the book chapter for details and examples.
- SQL `IN` clauses are now handled via `Fragments.in`, which is a **breaking change** relative to 0.3.0. See the book chapter on parameterized queries for an example.
- Methods on `Query[0]/Update[0]` that construct streams (`.process`) now have variants that allow you to specify the **chunk size**, which by default is 512 rows.
- There is now an `IOLite` data type that you can use if you're having a hard time settling on a target effect type. It works identically in Cats and scalaz and is what's used in the book.

##### Changes to Add-On Modules

We cleaned up the add-on modules a bit and made some naming simplifications that are **breaking** relative to 0.3.0.

- There is now a **ScalaTest** add-on. See the book chapter on unit testing for details and examples.
- The `contrib` segment has been removed from module names, so `doobie-contrib-h2` is now just `doobie-h2`. It has also been removed from *package* names. So `doobie.contrib.h2` is now just `doobie.h2`.
- In both cases the `postgresql` segment has been shortened to `postgres`.
- All modules now have a consistent import story. `import doobie.<module>.imports._` should get you everything you need.

That's it! Enjoy the release. Once again many thanks to our contributors.

----

### <a name="0.3.0"></a>New and Noteworthy for Version 0.3.0

This release brings **doobie** up to date with major dependencies, almost entirely due to the hard work of **@guersam**. The release is otherwise equivalent to 0.2.4 from a feature standpoint.

Upgrades:
- Updated to scalaz 7.2
- Updated to shapeless 2.3
- Updated to JDK 1.8
- Added build support Scala 2.12


----

### <a name="0.2.4"></a>New and Noteworthy for Version 0.2.4

This is a minor release with a few odds and ends and an important library update.

Improvements:
- You can now construct a `HikariTransactor` from an existing `HikariDataSource` (thanks **@raulraja**).
- There is now a `.nel` accumulator for non-empty resultsets (thanks **@refried**).
- Arrays of `UUID`s are now supported for PostgreSQL (thanks **@veegee**).
- Published jarfiles now have OSGi headers.
- Some internal cleanup but nothing that should affect end users.

Upgrades:
- Updated to scalaz-stream 0.8 (thanks **@guersam**).


----

### <a name="0.2.3"></a>New and Noteworthy for Version 0.2.3

This release includes more performance work and some usability improvements, as well as documentation improvements here and there. This release should be source-compatible for most users, but is **not** binary compatible with 0.2.2 or any other release. Let me know if you run into source compatibilty problems. Special thanks to **@mdmoss** for build improvements, **@fommil** and **@non** for help with Sonatype, and everyone else for your continued interest and contributions!

Improvements:

- **doobie** is now published on Sonatype and no longer requires a Bintray resolver.
- The `free` modules now provide natural transformations of the form `FooIO ~> Kleisli[M, Foo, ?]` and `Foo => FooIO ~> M`, which should make life easier when using **doobie** with existing JDBC resources (your own `Connection` for example).
- New optimized column-vector reads and accumulators for `IList` and standard library collections via `CanBuildFrom` yield performance generally indistinguishable from raw JDBC. The `Query/Query0` operations `to`, `list`, and `vector` are now very fast. Result handling via `Process` benefits to a lesser extent.
- `Composite` instances are now available for shapeless record types.
- `Atom` instances are now available for single-element product types (thanks **@wedens** and **@refried**).
- `DriverManagerTransactor` now includes constructors corresponding to all `getConnection` methods on `DriverManager`.
- `free` algebras and interpreters have been re-implemented to use method dispatch rather than large `match` expressions, resulting in minor performance improvements throughout. This completes the work started in 0.2.2.
- The `sql` interpolator now supports interpolated sequences for SQL `IN` clauses. See [Chapter 5](http://tpolecat.github.io/doobie-0.2.3/05-Parameterized.html) for more information.
- The **book of doobie** now includes a [FAQ Chapter](http://tpolecat.github.io/doobie-0.2.3/15-FAQ.html).
- The `example` project now includes some PostgreSQL CopyManager examples (thanks **@wedens**).

Big Fixes:

- The PostGIS dependency was pulling in unnecessary transitive dependencies that caused problems with sbt-assembly. This has been fixed.

Upgrades:

- Updated to Scala 2.11.7
- Updated to shapeless 2.2.5
- Updated to scalaz-stream 0.7.2a
- Updated to specs2-core 3.6
- Updated to tut 0.4.0 (build-time dependency only)
- Updated to sbt 0.13.8 (build-time dependency only)
- Updated to kind-projector 0.7.1 (build-time dependency only)


----

### <a name="0.2.2"></a>New and Noteworthy for Version 0.2.2

This is another minor release that adds yet more support PostgreSQL-specific features, updates dependencies, and improves performance for resultset processing. Thanks everyone for your continued interest and contributions!

Additions:

- Added `HC.updateManyWithGeneratedKeys` and associated syntax on `Update` to allow batch updates to return updated rows. See [Chapter 7](http://tpolecat.github.io/doobie-0.2.2/07-Updating.html) for an example.
- Added algebras and free monads thereof for PostgreSQL vendor-specific driver APIs, which allows **doobie** to directly support `LISTEN/NOTIFY`, `COPY FROM STDIN` and a number of other interesting features. See [Chapter 13](http://tpolecat.github.io/doobie-0.2.2/13-Extensions-PostgreSQL.html) for details and examples.

Improvements:

- Huge improvements to the implementation of the `sql` interpolator, courtesy of [@milessabin](https://twitter.com/milessabin). This removes the last remaining arity limit.
- Added examples for PostgreSQL-specific error handling combinators. See [Chapter 13](http://tpolecat.github.io/doobie-0.2.2/13-Extensions-PostgreSQL.html) for more information.
- Added `Unapply` instances to make the `Monad` instance for `FreeC` and associated syntax conversions inferrable. For now these are on the `doobie.imports` module.
- Significant performance improvements for resultset processing, with `.list` and `.vector` now ~4.5x faster and `.process` ~2.5x faster. This work is ongoing.

Upgrades:

- Updated to Scala 2.10.5
- Updated to shapeless 2.2.0 (thanks [@milessabin](https://twitter.com/milessabin))
- Updated to scalaz-stream 0.7a
- Updated to PostgreSQL JDBC driver 9.4-1201-jdbc41
- Updated to Specs2 3.6 (thanks [@etorrebore](https://twitter.com/etorreborre))


----

### <a name="0.2.1"></a>New and Noteworthy for Version 0.2.1

This is a minor follow-up release, primarily to add support for some PostgreSQL features and other odds and ends reported by users. Thanks to users and contributors for their help!

Additions:

- Added `Transactor` to wrap an existing `DataSource`. See [Chapter 12](http://tpolecat.github.io/doobie-0.2.1/12-Managing-Connections.html) for more information.
- Added support for PostGIS and PostgreSQL `enum` types. See [Chapter 13](http://tpolecat.github.io/doobie-0.2.1/13-Extensions-PostgreSQL.html) for more information.
- Added combinators for batch updates. See [Chapter 7](http://tpolecat.github.io/doobie-0.2.1/07-Updating.html) for more information.
- Added `Composite` support for `HList`; anywhere you could map a product or tuple type in 0.2.0 you can now also use a [shapeless](https://github.com/milessabin/shapeless) `HList`.
- Added `Atom` support for `scalaz.Maybe`; anywhere you could map an `Option` type in 0.2.0 you can now also use `Maybe`.
- Added `.optionT` method on `Query` and `Query0`.
- Added an [example](https://github.com/tpolecat/doobie/blob/v0.2.1/example/src/main/scala/example/PostgresNotify.scala) that exposes a PostgreSQL `NOTIFY` channel as an scalaz-stream `Process`.

Improvements:

- The 22-parameter limit on the `sql` interpolator has been increased to 50, and should go away entirely by 0.3.0 at the latest. There are no other arity limits in **doobie**.
- All `Query` and `Update` constructions are now supported for typechecking with Specs2 and YOLO mode.
- Many improvements in **book of doobie**.
- Tidied up examples a bit.

Upgrades:

- Updated to Scala 2.11.6.
- Updated to scalaz 7.1.1 and scalaz-stream 0.6a
- Updated to [tut](https://github.com/tpolecat/tut) 0.3.1 (build only; not user-facing).

Bug Fixes:

- Fixed problem with typechecking `BOOLEAN` column mappings.
- Fixed driver classloading problem with `HikariTransactor`.
