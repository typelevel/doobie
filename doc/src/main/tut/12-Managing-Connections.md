---
layout: book
number: 12
title: Managing Connections
---

### Using the `java.sql.DriverManager`

### Using HikariCP

The `doobie-contrib-hikari` add-on provides a `Transactor` implementation backed by a [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pool. Constructing an instance is an effect:

```scala
for {
  xa <- HikariTransactor[Task]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
  _  <- longRunningProgram(xa) ensuring xa.shutdown
} yield ()
```

The returned instance is of type `HikariTransactor`, which provides a `shutdown` method (shown above) as well as a `configure` method that provides access to the underlying `HikariDataSource`. See the source for details.

### Using an Existing `java.sql.Connection`


### Using an Existing `javax.sql.DataSource`


### Building your own `Transactor`



