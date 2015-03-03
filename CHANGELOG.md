# changelog

This file summarizes **notable** changes for each release. For complete details see the corresponding [milestones](https://github.com/tpolecat/doobie/milestones).

### <a name="0.2.1"></a>New and Noteworthy for Version 0.2.1

Improvements:

- 22-parameter limit on `sql` interpolator increased to 50; with some luck should go away entirely in the next version. There are no other arity limits in **doobie**.
- Ensured that all `Query` and `Update` constructions are supported for typechecking with Specs2 and YOLO mode.
- Miscellaneous improvements in **book of doobie**; added chapter for connection management and expanded vendor extensions into multiple chapters.

Additions:

- Added `Transactor` to wrap an existing `DataSource`.
- Added support for PostGIS and Postgres `enum` types. 
- Added composite support for `HList`. This is pretty self-explanatory; anywhere you could use a product or tuple type in 0.2.0 you can now also use an `HList`.
- Added support for `scalaz.Maybe` for nullable columns.
- Added combinators for batch updates.
- Added `optionT` method on `Query` and `Query0`.

Upgrades:

- Updated to scalaz 7.1 and scalaz-stream 0.6a
- Updated to tut 0.3.1 (build only; not user-facing).

