# changelog

This file summarizes **notable** changes for each release, but does not describe internal changes unless they are particularly exciting. For complete details please see the corresponding [milestones](https://github.com/tpolecat/doobie/milestones?state=closed) and their associated issues.

### <a name="0.3.0-M1"></a>New and Noteworthy for Version 0.3.0-M1

This will be a **major release** with **breaking changes** to come. 

Special thanks to **@guersam** for updating the build and all the dependencies; this was nontrivial.

- **doobie** artifacts now include OSGi headers.
- Added `.nel` handlers for reading result sets that are expected to be no-empty.

Upgrades:

- Updated to Scala 2.11.8 and 2.12.0-M3
- Updated to shapeless 2.3.0
- Updated to scalaz 7.2.0
- Updated to scalaz-stream 0.8a

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
