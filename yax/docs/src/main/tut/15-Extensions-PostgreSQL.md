---
layout: book
number: 15
title: Extensions for PostgreSQL
---

In this chapter we discuss the extended support that **doobie** offers for users of [PostgreSQL](http://www.postgresql.org/). To use these extensions you must add an additional dependency to your project:

```scala
libraryDependencies += "org.tpolecat" %% "doobie-contrib-postgresql" % "0.2.2"
```

This library pulls in [PostgreSQL JDBC Driver 9.4](https://jdbc.postgresql.org/documentation/94/index.html) as a transitive dependency.

### Setting Up

The following examples require a few imports.

```tut:silent
import doobie.imports._
#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.data._, cats.implicits._
import fs2.interop.cats._
#-cats
val xa = DriverManagerTransactor[IOLite](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
val y = xa.yolo; import y._
```

**doobie** adds support for a large number of extended types that are not supported directly by JDBC. All mappings (except postgis) are provided in the `pgtypes` module.

```tut:silent
import doobie.postgres.imports._
```

### Array Types

**doobie** supports single-dimensional arrays of the following types:

- `bit[]` maps to `Array[Boolean]`
- `int4[]` map to `Array[Int]`
- `int8[]` maps to `Array[Long]`
- `float4[]` maps to `Array[Float]`
- `float8[]` maps to `Array[Double]`
- `varchar[]`, `char[]`, `text[],` and `bpchar[]` all map to `Array[String]`.

In addition to `Array` you can also map to `List` and `Vector`. Note that arrays of advanced types and structs are not supported by the driver; arrays of `Byte` are represented as `bytea`; and arrays of `int2` are incorrectly mapped by the driver as `Array[Int]` rather than `Array[Short]` and are not supported in **doobie**.

See the previous chapter on **SQL Arrays** for usage examples.

### Enum Types

**doobie** supports mapping PostgreSQL `enum` types to Scala enumerated types, with the slight complication that Scala doesn't really support enumerated types as a first-class notion. We will examine three ways to construct mappings for the following PostgreSQL type:

```sql
create type myenum as enum ('foo', 'bar')
```

**NOTE** that because it seems to be impossible to write a `NULL` value to an enum column or parameter, **doobie** cannot support `Option` mappings for enum types.

The first option is to map `myenum` to an instance of the execrable `scala.Enumeration` class via the `pgEnum` constructor.

```tut:silent
object MyEnum extends Enumeration {
  val foo, bar = Value
}

implicit val MyEnumAtom = pgEnum(MyEnum, "myenum")
```

```tut
sql"select 'foo'::myenum".query[MyEnum.Value].unique.quick.unsafePerformIO
```

It works, but `Enumeration` is terrible so it's unlikely you will want to do this. A better option, perhaps surprisingly, is to map `myenum` to a **Java** `enum` via the `pgJavaEnum` constructor.

```java
// This is Java code
public enum MyJavaEnum { foo, bar; }
```

```scala
implicit val MyJavaEnumAtom = pgJavaEnum[MyJavaEnum]("myenum")
```

And the final, most general construction simply requires evidence that your taget type can be translated to and from `String`.

```tut:silent
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

  def unsafeFromEnum(s: String): FooBar =
    fromEnum(s).getOrElse(throw doobie.util.invariant.InvalidEnum[FooBar](s))

}

implicit val FoobarAtom: Atom[FooBar] =
  pgEnumString("myenum", FooBar.unsafeFromEnum, FooBar.toEnum)
```

```tut
sql"select 'foo'::myenum".query[FooBar].unique.quick.unsafePerformIO
```


### Geometric Types

The following geometric types are supported, and map to driver-supplied types.

- the `box` schema type maps to `org.postgresql.geometric.PGbox`
- the `circle` schema type maps to `org.postgresql.geometric.PGcircle`
- the `lseg` schema type maps to `org.postgresql.geometric.PGlseg`
- the `path` schema type maps to `org.postgresql.geometric.PGpath`
- the `point` schema type maps to `org.postgresql.geometric.PGpoint`
- the `polygon` schema type maps to `org.postgresql.geometric.PGpolygon`

It is expected that these will be mapped to application-specific types via `nxmap` as described in **Custom Mappings**.

### PostGIS Types

**doobie** provides mappings for the top-level PostGIS geometric types provided by the `org.postgis` driver extension.

Mappings for postgis are provided in the `pgistypes` module. Doobie expects postgis dependency to be provided, so if you use this module you should add postgis as a dependency.

```scala
libraryDependencies += "org.postgis" % "postgis-jdbc" % "1.3.3"
```

```tut:silent
// Not provided via doobie.postgres.imports._; you must import them explicitly.
import doobie.postgres.pgistypes._
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

### Extended Error Handling

A complete table of SQLSTATE values is provided in the `doobie.postgres.sqlstate` module. Recovery combinators for each of these states (`onUniqueViolation` for example) are provided in `doobie.postgres.syntax`.

```tut:silent
import doobie.postgres.imports._

val p = sql"oops".query[String].unique // this won't work
```

Some of the recovery combinators demonstrated:

```tut
p.attempt.quick.unsafePerformIO // attempt provided by Catchable instance

p.attemptSqlState.quick.unsafePerformIO // this catches only SQL exceptions

p.attemptSomeSqlState { case SqlState("42601") => "caught!" } .quick.unsafePerformIO // catch it

p.attemptSomeSqlState { case sqlstate.class42.SYNTAX_ERROR => "caught!" } .quick.unsafePerformIO // same, w/constant

p.exceptSomeSqlState { case sqlstate.class42.SYNTAX_ERROR => "caught!".pure[ConnectionIO] } .quick.unsafePerformIO // recover

p.onSyntaxError("caught!".pure[ConnectionIO]).quick.unsafePerformIO // using recovery combinator
```


### Server-Side Statements

PostgreSQL supports server-side caching of prepared statements after a certain number of executions, which can have desirable performance consequences for statements that only need to be planned once. Note that this caching happens only for `PreparedStatement` instances that are re-used within a single connection lifetime. **doobie** supports programmatic configuration of the prepare threshold:

- For a given `Connection` you can set and query the prepare threshold with the `ConnectionIO` constructors `doobie.postgres.hi.connection.pgSetPrepareThreshold` and `pgGetPrepareThreshold`.
- For a specific `PreparedStatement` you can set and query the prepare threshold with the `PreparedStatementIO` constructors `doobie.postgres.hi.preparedstatement.pgSetPrepareThreshold` and `pgGetPrepareThreshold`.

See the [JDBC driver documentation](https://jdbc.postgresql.org/documentation/93/server-prepare.html) for more information.

### `LISTEN` and `NOTIFY`

PostgreSQL provides a simple transactional message queue that can be used to notify a connection that something interesting has happened. Such notifications can be tied to database triggers, which provides a way to notify clients that data has changed. Which is cool.

**doobie** provides `ConnectionIO` constructors for SQL `LISTEN`, `UNLISTEN`, and `NOTIFY` in the `doobie.postgres.hi.connection` module. New notifications are retrieved (synchronously, sadly, that's all the driver provides) via `pgGetNotifications`. Note that all of the "listening" operations apply to the **current connection**, which must therefore be long-running and typically off to the side from normal transactional operations. Further note that you must `setAutoCommit(false)` on this connection or `commit` between each call in order to retrieve messages. The `examples` project includes a program that demonstrates how to present a channel as a `Process[Task, PGNotification]`.

### Large Objects

PostgreSQL provides a facility for storing very large objects (up to 4TB each) in a single uniform storage, identified by unique numeric ID and accessed via fast byte-block transfer. Note that "normal" large object columns types such as `bytea` and `text` can store values as large as 1GB each, so the large object API is rarely used. However there are cases where the size and/or efficiency of large objects justifies the use of this API.

**doobie** provides an algebra and free monads for the driver's `LargeObjectManager` and `LargeObject` types in the `doobie.postgres.free` package. There is also [the beginnings of] a high-level API that includes constructors for creating large objects from files and vice-versa. The `example` project contains a brief usage example.

Please file an issue or ask questions on the [Gitter](https://gitter.im/tpolecat/doobie) channel if you need to use this API; it will evolve as use cases demand.

### Copy Manager

The PostgreSQL JDBC driver's [CopyManager](https://jdbc.postgresql.org/documentation/publicapi/org/postgresql/copy/CopyManager.html) API provides a pass-through for the SQL [`COPY`](http://www.postgresql.org/docs/9.3/static/sql-copy.html) statement, allowing very fast data transfer via `java.io` streams. Here we construct a program that dumps a table to `Console.out` in CSV format, with quoted values.

```tut:silent
import doobie.postgres.imports._

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

### Fastpath

**doobie** provides an algebra and free monad for constructing programs that use the `FastPathAPI` provided by the PostgreSQL JDBC driver, however this API is mostly deprecated in favor of server-side statements (see above). And in any case I can't find an example of how you would use it from Java so I don't have an example here. But if you're using it let me know and we can figure it out.
