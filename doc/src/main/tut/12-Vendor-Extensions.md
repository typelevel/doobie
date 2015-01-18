---
layout: book
number: 12
title: Vendor Extensions
---

### HikariCP Support

The `contrib-hikari` add-on provides a `Transactor` backed by a `HikariDataSource`.

```scala
for {
  xa <- HikariTransactor[Task]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
  _  <- longRunningProgram(xa) ensuring xa.shutdown
} yield ()
```

### H2 Support

The `contrib-h2` add-on provides mappings for the following H2 types in the `doobie.contrib.h2.h2types` module.

- H2 `array` types are supported and map to `Array`, `List`, `Vector`.
- The `uuid` type is supported and maps to `java.util.UUID`.

### PostgreSQL Support

The `contrib-postgres` add-on provides mappings for the following PostgreSQL types in the `doobie.contrib.postgresql.pgtypes` module.

- Postgres typed arrays are supported and map to `Array`, `List`, `Vector`. Multi-dimensional arrays are not supported yet.
- The `uuid` type is supported and maps to `java.util.UUID`.
- The `inet` type is supported and maps to `java.net.InetAddress`.
- The following geometric types are supported, and map to driver-supplied types. These will normally be mapped to application-specific types.
  - the `box` maps to `org.postgresql.geometric.PGbox`
  - the `circle` maps to `org.postgresql.geometric.PGcircle`
  - the `lseg` maps to `org.postgresql.geometric.PGlseg`
  - the `path` maps to `org.postgresql.geometric.PGpath`
  - the `point` maps to `org.postgresql.geometric.PGpoint`
  - the `polygon` maps to `org.postgresql.geometric.PGpolygon`

A complete table of SQLSTATE values is provided in the `doobie.contrib.postgresql.sqlstate` module, and recovery combinators for each of these (`onUniqueViolation` for example) are provided in `doobie.contrib.postgresql.syntax`. 

