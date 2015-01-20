---
layout: book
number: 12
title: Vendor Extensions
---

Version 0.2.0 introduces the `doobie-contrib` family of add-on libraries, which are published concurrently with the **doobie** release and provide support for functionality outside the JDBC specification. These can be added as needed:

```scala
libraryDependencies += "org.tpolecat" %% "doobie-contrib-XXX" % "0.2.0" // for some XXX
```

### HikariCP Support

The `doobie-contrib-hikari` add-on provides a `Transactor` implementation backed by a [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pool. Constructing an instance is an effect:

```scala
for {
  xa <- HikariTransactor[Task]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
  _  <- longRunningProgram(xa) ensuring xa.shutdown
} yield ()
```

The returned instance is of type `HikariTransactor`, which provides a `shutdown` method (shown above) as well as a `configure` method that provides access to the underlying `HikariDataSource`. See the source for details.


### H2 Support

The `doobie-contrib-h2` add-on provides mappings for the following [H2](http://www.h2database.com/html/main.html) types in the `doobie.contrib.h2.h2types` module.

- H2 `array` types are supported and map to `Array`, `List`, `Vector`.
- The `uuid` type is supported and maps to `java.util.UUID`.

### PostgreSQL Support

The `doobie-contrib-postgres` add-on provides mappings for the following PostgreSQL types in the `doobie.contrib.postgresql.pgtypes` module.

- Postgres typed arrays are supported and map to `Array`, `List`, `Vector`. Multi-dimensional arrays are not supported yet.
- The `uuid` schema type is supported and maps to `java.util.UUID`.
- The `inet` schema type is supported and maps to `java.net.InetAddress`.
- The following geometric types are supported, and map to driver-supplied types. These will normally be mapped to application-specific types.
  - the `box` schema type maps to `org.postgresql.geometric.PGbox`
  - the `circle` schema type maps to `org.postgresql.geometric.PGcircle`
  - the `lseg` schema type maps to `org.postgresql.geometric.PGlseg`
  - the `path` schema type maps to `org.postgresql.geometric.PGpath`
  - the `point` schema type maps to `org.postgresql.geometric.PGpoint`
  - the `polygon` schema type maps to `org.postgresql.geometric.PGpolygon`

A complete table of SQLSTATE values is provided in the `doobie.contrib.postgresql.sqlstate` module, and recovery combinators for each of these (`onUniqueViolation` for example) are provided in `doobie.contrib.postgresql.syntax`. 

### Specs2 Support

The `doobie-contrib-specs2` add-on provides a mix-in trait for Specs2 tests that provides support for query validation. See the chapter on unit testing for more information.




