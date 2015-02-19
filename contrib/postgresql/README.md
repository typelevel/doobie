# PostgreSQL Extensions for Doobie

This project provides (currently) a handful of PostgreSQL-specific extensions.

* A module of PostgreSQL SQLSTATE values, and a module of `Catchable` combinators for each type. This allows the user to say `doThis.onUniqueViolation(doThat)` for example.
* `Meta` instances for a number of PostgreSQL-specific types like `point` and `inet`, as well as support for underspecified "advanced" types from the JDBC specification (at this point just `ARRAY`).
* `Meta` instances for PostGIS types.

The intent is to eventually support:

* All driver-supported data types.
* `COPY TO` and `COPY FROM` bulk transfer.
* Fastpath function interface.
* Native large object interface.
* Server notifications.

In order to set up to run the tests, do something like the following. The tests expect a `world` database with PostGIS enabled, accessable r/w by a `postgres` user with no password.

```
$ psql -c 'create user postgres createdb'
$ psql -c 'create database world;' -U postgres
$ psql -c '\i world.sql' -d world -U postgres
$ psql -d world -c "create extension postgis" -U postgres
```

