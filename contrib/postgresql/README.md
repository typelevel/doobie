# PostgreSQL Extensions for Doobie

This project provides (currently) a handful of PostgreSQL-specific extensions.

* A module of PostgreSQL SQLSTATE values, and a module of `Catchable` combinators for each type. This allows the user to say `doThis.onUniqueViolation(doThat)` for example.
* `ScalaType` instances for a number of PostgreSQL-specific types like `point` and `inet`, as well as support for underspecified "advanced" types from the JDBC specification (at this point just `ARRAY`).

The intent is to eventually support:

* All driver-supported data types.
* `COPY TO` and `COPY FROM` bulk transfer.
* Fastpath function interface.
* Native large object interface.
* Server notifications.

