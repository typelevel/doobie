---
layout: docs
number: 15
title: Extensions for PostgreSQL
---

## {{page.title}}

In this chapter we discuss the extended support that **doobie** offers for users of [PostgreSQL](http://www.postgresql.org/). To use these extensions you must add an additional dependency to your project:

```scala
libraryDependencies += "org.tpolecat" %% "doobie-postgres" % "{{site.doobieVersion}}"
```

This library pulls in [PostgreSQL JDBC Driver](https://jdbc.postgresql.org) as a transitive dependency.

### Setting Up

The following examples require a few imports.

```scala mdoc:silent
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts

// We need a ContextShift[IO] before we can construct a Transactor[IO]. The passed ExecutionContext
// is where nonblocking operations will be executed. For testing here we're using a synchronous EC.
implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

// A transactor that gets connections from java.sql.DriverManager and executes blocking operations
// on an our synchronous EC. See the chapter on connection handling for more info.
val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver",     // driver classname
  "jdbc:postgresql:world",     // connect URL (driver-specific)
  "postgres",                  // user
  "",                          // password
  Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
)
```

```scala mdoc:invisible
implicit val mdocColors: doobie.util.Colors = doobie.util.Colors.None
```

**doobie** adds support for a large number of extended types that are not supported directly by JDBC. All mappings (except postgis) are provided in the `pgtypes` module.

```scala mdoc:silent
import doobie.postgres._
import doobie.postgres.implicits._
```

### Array Types

**doobie** supports single-dimensional arrays of the following types:

- `bit[]` maps to `Array[Boolean]`
- `int4[]` map to `Array[Int]`
- `int8[]` maps to `Array[Long]`
- `float4[]` maps to `Array[Float]`
- `float8[]` maps to `Array[Double]`
- `varchar[]`, `char[]`, `text[],` and `bpchar[]` all map to `Array[String]`.
- `uuid[]` maps to `Array[UUID]`

In addition to `Array` you can also map to `List` and `Vector`. Note that arrays of advanced types and structs are not supported by the driver; arrays of `Byte` are represented as `bytea`; and arrays of `int2` are incorrectly mapped by the driver as `Array[Int]` rather than `Array[Short]` and are not supported in **doobie**.

See the previous chapter on **SQL Arrays** for usage examples.

### Enum Types

**doobie** supports mapping PostgreSQL `enum` types to Scala enumerated types, with the slight complication that Scala doesn't really support enumerated types as a first-class notion. We will examine three ways to construct mappings for the following PostgreSQL type:

```sql
create type myenum as enum ('foo', 'bar')
```

The first option is to map `myenum` to an instance of the execrable `scala.Enumeration` class via the `pgEnum` constructor.

```scala mdoc:silent
object MyEnum extends Enumeration {
  val foo, bar = Value
}

implicit val MyEnumMeta = pgEnum(MyEnum, "myenum")
```

```scala mdoc
sql"select 'foo'::myenum".query[MyEnum.Value].unique.transact(xa).unsafeRunSync
```

It works, but `Enumeration` is terrible so it's unlikely you will want to do this. A better option, perhaps surprisingly, is to map `myenum` to a **Java** `enum` via the `pgJavaEnum` constructor.

```java
// This is Java code
public enum MyJavaEnum { foo, bar; }
```

```scala
implicit val MyJavaEnumMeta = pgJavaEnum[MyJavaEnum]("myenum")
```

And the final, most general construction simply requires evidence that your taget type can be translated to and from `String`.

```scala mdoc:silent
sealed trait FooBar

object FooBar {

  case object Foo extends FooBar
  case object Bar extends FooBar

  def toEnum(e: FooBar): String =
    e match {
      case Foo => "foo"
      case Bar => "bar"
    }

  def fromEnum(s: String): Option[FooBar] =
    Option(s) collect {
      case "foo" => Foo
      case "bar" => Bar
    }

}

implicit val FoobarMeta: Meta[FooBar] =
  pgEnumStringOpt("myenum", FooBar.fromEnum, FooBar.toEnum)
```

```scala mdoc
sql"select 'foo'::myenum".query[FooBar].unique.transact(xa).unsafeRunSync
```


### Geometric Types

The following geometric types are supported, and map to driver-supplied types.

- the `box` schema type maps to `org.postgresql.geometric.PGbox`
- the `circle` schema type maps to `org.postgresql.geometric.PGcircle`
- the `lseg` schema type maps to `org.postgresql.geometric.PGlseg`
- the `path` schema type maps to `org.postgresql.geometric.PGpath`
- the `point` schema type maps to `org.postgresql.geometric.PGpoint`
- the `polygon` schema type maps to `org.postgresql.geometric.PGpolygon`

It is expected that these will be mapped to application-specific types via `xmap` as described in **Custom Mappings**.

### PostGIS Types

**doobie** provides mappings for the top-level PostGIS geometric types provided by the `org.postgis` driver extension.

Mappings for postgis are provided in the `pgistypes` module. Doobie expects postgis dependency to be provided, so if you use this module you should add postgis as a dependency.

```scala
libraryDependencies += "net.postgis" % "postgis-jdbc" % "2.3.0"
```

```scala mdoc:silent
// Not provided via doobie.postgres.imports._; you must import them explicitly.
import doobie.postgres.pgisimplicits._
```

- `PGgeometry`
- `PGbox2d`
- `PGbox3d`

In addition to the general types above, **doobie** provides mappings for the following abstract and concrete fine-grained types carried by `PGgeometry`:

- `Geometry`
- `ComposedGeom`
- `GeometryCollection`
- `MultiLineString`
- `MultiPolygon`
- `PointComposedGeom`
- `LineString`
- `MultiPoint`
- `Polygon`
- `Point`

### Other Nonstandard Types

- The `uuid` schema type is supported and maps to `java.util.UUID`.
- The `inet` schema type is supported and maps to `java.net.InetAddress`.
- The `hstore` schema type is supported and maps to both `java.util.Map[String, String]` and Scala `Map[String, String]`.

### Extended Error Handling

A complete table of SQLSTATE values is provided in the `doobie.postgres.sqlstate` module. Recovery combinators for each of these states (`onUniqueViolation` for example) are provided in `doobie.postgres.syntax`.

```scala mdoc:silent
val p = sql"oops".query[String].unique // this won't work
```

Some of the recovery combinators demonstrated:

```scala mdoc
p.attempt.transact(xa).unsafeRunSync // attempt is provided by ApplicativeError instance

p.attemptSqlState.transact(xa).unsafeRunSync // this catches only SQL exceptions

p.attemptSomeSqlState { case SqlState("42601") => "caught!" } .transact(xa).unsafeRunSync // catch it

p.attemptSomeSqlState { case sqlstate.class42.SYNTAX_ERROR => "caught!" } .transact(xa).unsafeRunSync // same, w/constant

p.exceptSomeSqlState { case sqlstate.class42.SYNTAX_ERROR => "caught!".pure[ConnectionIO] } .transact(xa).unsafeRunSync // recover

p.onSyntaxError("caught!".pure[ConnectionIO]).transact(xa).unsafeRunSync // using recovery combinator
```


### Server-Side Statements

PostgreSQL supports server-side caching of prepared statements after a certain number of executions, which can have desirable performance consequences for statements that only need to be planned once. Note that this caching happens only for `PreparedStatement` instances that are re-used within a single connection lifetime. **doobie** supports programmatic configuration of the prepare threshold:

- For a given `Connection` you can set and query the prepare threshold with the `ConnectionIO` constructors `doobie.postgres.hi.connection.pgSetPrepareThreshold` and `pgGetPrepareThreshold`.
- For a specific `PreparedStatement` you can set and query the prepare threshold with the `PreparedStatementIO` constructors `doobie.postgres.hi.preparedstatement.pgSetPrepareThreshold` and `pgGetPrepareThreshold`.

See the [JDBC driver documentation](https://jdbc.postgresql.org/documentation/93/server-prepare.html) for more information.

### `LISTEN` and `NOTIFY`

PostgreSQL provides a simple transactional message queue that can be used to notify a connection that something interesting has happened. Such notifications can be tied to database triggers, which provides a way to notify clients that data has changed. Which is cool.

**doobie** provides `ConnectionIO` constructors for SQL `LISTEN`, `UNLISTEN`, and `NOTIFY` in the `doobie.postgres.hi.connection` module. New notifications are retrieved (synchronously, sadly, that's all the driver provides) via `pgGetNotifications`. Note that all of the "listening" operations apply to the **current connection**, which must therefore be long-running and typically off to the side from normal transactional operations. Further note that you must `setAutoCommit(false)` on this connection or `commit` between each call in order to retrieve messages. The `examples` project includes a program that demonstrates how to present a channel as a `Stream[IO, PGNotification]`.

### Large Objects

PostgreSQL provides a facility for storing very large objects (up to 4TB each) in a single uniform storage, identified by unique numeric ID and accessed via fast byte-block transfer. Note that "normal" large object columns types such as `bytea` and `text` can store values as large as 1GB each, so the large object API is rarely used. However there are cases where the size and/or efficiency of large objects justifies the use of this API.

**doobie** provides an algebra and free monads for the driver's `LargeObjectManager` and `LargeObject` types in the `doobie.postgres.free` package. There is also [the beginnings of] a high-level API that includes constructors for creating large objects from files and vice-versa. The `example` project contains a brief usage example.

Please file an issue or ask questions on the [Gitter](https://gitter.im/tpolecat/doobie) channel if you need to use this API; it will evolve as use cases demand.

### Copy Manager

The PostgreSQL JDBC driver's [CopyManager](https://jdbc.postgresql.org/documentation/publicapi/org/postgresql/copy/CopyManager.html) API provides a pass-through for the SQL [`COPY`](http://www.postgresql.org/docs/9.3/static/sql-copy.html) statement, allowing very fast data transfer via `java.io` streams. Here we construct a program that dumps a table to `Console.out` in CSV format, with quoted values.

```scala mdoc:silent
val q = """
  copy country (name, code, population)
  to stdout (
    encoding 'utf-8',
    force_quote *,
    format csv
  )
  """

val prog: ConnectionIO[Long] =
  PHC.pgGetCopyAPI(PFCM.copyOut(q, Console.out)) // return value is the row count
```

See the links above and sample code in the `examples/` project in the **doobie** GitHub repo for more information on this specialized API.

<div class="alert alert-danger" role="alert">
Note: the following API was introduced in version 0.5.2 and is experimental. Community feedback and contributions (instances in particular) are encouraged.
</div>

**doobie** also provides a specialized API for very fast batch inserts using upates of the form `COPY ... FROM STDIN` and a `Text` typeclass that defines how data types are encoded in Postgres text format (similar to `Put`; instances must be present for all fields in the data type to be inserted).

First a temp table for our experiment.

```scala mdoc:silent
val create: ConnectionIO[Unit] =
  sql"""
    CREATE TEMPORARY TABLE food (
      name       VARCHAR,
      vegetarian BOOLEAN,
      calories   INTEGER
    )
  """.update.run.void
```

And some values to insert. `Text` instances are provided for all the data types we are using here.

```scala mdoc:silent
case class Food(name: String, isVegetarian: Boolean, caloriesPerServing: Int)

val foods = List(
  Food("banana", true, 110),
  Food("cheddar cheese", true, 113),
  Food("Big Mac", false, 1120)
)
```

Our insert statement must have the form `COPY ... FROM STDIN`, and we can insert any `Foldable`.

```scala mdoc:silent
def insert[F[_]: Foldable](fa: F[Food]): ConnectionIO[Long] =
  sql"COPY food (name, vegetarian, calories) FROM STDIN".copyIn(fa)
```

We can run it thus, yielding the number of affected rows.

```scala mdoc
(create *> insert(foods)).transact(xa).unsafeRunSync
```

### Fastpath

**doobie** provides an algebra and free monad for constructing programs that use the `FastPathAPI` provided by the PostgreSQL JDBC driver, however this API is mostly deprecated in favor of server-side statements (see above). And in any case I can't find an example of how you would use it from Java so I don't have an example here. But if you're using it let me know and we can figure it out.
