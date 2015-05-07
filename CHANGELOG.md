# changelog

This file summarizes **notable** changes for each release, but does not describe internal changes unless they are particularly exciting. For complete details please see the corresponding [milestones](https://github.com/tpolecat/doobie/milestones) and their associated issues.

### <a name="0.2.2"></a>New and Noteworthy for Version 0.2.2

Additions:

- Added `HC.updateManyWithGeneratedKeys` and associated syntax on `Update` to allow batch updates to return updated rows. See [Chapter 7](http://tpolecat.github.io/doobie-0.2.2/07-Updating.html) for an example.
- Added algebras and free monads for PostgreSQL vendor-specific driver APIs, which allows **doobie** to directly support `LISTEN/NOTIFY`, `COPY FROM STDIN` and a number of other interesting features. See [Chapter 13](http://tpolecat.github.io/doobie-0.2.2/13-Extensions-PostgreSQL.html) for details and exampls.

Improvements:

- Huge improvements to the implementation of the `sql` interpolator, courtesy of @milessabin. This removes the current 50-parameter arity limit.
- Added examples of Postgres-specific error handling. See [Chapter 13](http://tpolecat.github.io/doobie-0.2.2/13-Extensions-PostgreSQL.html) for more information.
- Added `Unapply` instances to make the `Monad` instance for `FreeC` and associated syntax conversions inferrable.

Upgrades:

- Updated to Scala 2.10.5
- Updated to shapeless 2.2.0
- Updated to scalaz-stream 0.7a
- Updated to PostgreSQL JDBC driver 9.4-1201-jdbc41

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
