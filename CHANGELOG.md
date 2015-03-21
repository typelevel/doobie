# changelog

This file summarizes **notable** changes for each release, but does not describe internal changes unless they are particularly exciting. For complete details please see the corresponding [milestones](https://github.com/tpolecat/doobie/milestones) and their associated issues.

### <a name="0.2.1"></a>New and Noteworthy for Version 0.2.1

This is a minor follow-up release, primarily to add support for some PostgreSQL features and other odds and ends reported by users. Thanks to users and contributors for their help!

Additions:

- Added `Transactor` to wrap an existing `DataSource`. See [Chapter 12](http://tpolecat.github.io/doobie-0.2.1-SNAPSHOT/12-Managing-Connections.html) for more information.
- Added support for PostGIS and PostgreSQL `enum` types. See [Chapter 13](http://tpolecat.github.io/doobie-0.2.1-SNAPSHOT/13-Extensions-PostgreSQL.html) for more information.
- Added combinators for batch updates. See [Chapter 7](http://tpolecat.github.io/doobie-0.2.1-SNAPSHOT/07-Updating.html) for more information.
- Added `Composite` support for `HList`; anywhere you could map a product or tuple type in 0.2.0 you can now also use a [shapeless](https://github.com/milessabin/shapeless) `HList`.
- Added `Atom` support for `scalaz.Maybe`; anywhere you could map an `Option` type in 0.2.0 you can now also use `Maybe`.
- Added `.optionT` method on `Query` and `Query0`.
- Added an [example](https://github.com/tpolecat/doobie/blob/master/example/src/main/scala/example/PostgresNotify.scala) that exposes a PostgreSQL `NOTIFY` channel as an scalaz-stream `Process`.

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
