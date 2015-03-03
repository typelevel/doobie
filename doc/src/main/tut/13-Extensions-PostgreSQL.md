---
layout: book
number: 13
title: Extensions for PostgreSQL
---

In this chapter we discuss the extended support that **doobie** offers for users of [PostgreSQL](http://www.postgresql.org/). To use these extensions you must add an additional dependency to your project:

```scala
libraryDependencies += "org.tpolecat" %% "doobie-contrib-postgresql" % "0.2.1"
```

This library pulls in [PostgreSQL JDBC Driver 9.3](https://jdbc.postgresql.org/documentation/93/index.html) as a transitive dependency.

### Setting Up

The following examples require a few imports.

```tut:silent
import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
import xa.yolo._
```

**doobie** adds support for a large number of extended types that are not supported directly by JDBC. All mappings are provided in the `pgtypes` module.

```tut:silent
import doobie.contrib.postgresql.pgtypes._
```

### Array Types

Postgres typed arrays are supported and map to `Array`, `List`, `Vector`. Multi-dimensional arrays are not supported yet.

### Enum Types

**doobie** supports mapping PostgreSQL `enum` types to Scala enumerated types, with the slight complication that Scala doesn't really support enumerated types as a first-class notion. We will examine three ways to construct mappings for the following PostgreSQL type:

```sql
create type myenum as enum ('foo', 'bar')
```

**NOTE** that because it seems to be impossible to write a `NULL` value to an enum column or parameter, **doobie** cannot support `Option` mappings for enum types.

The first option is to map `myenum` to an instance of the execrable `scala.Enumeration` class via the `pgEnum` constructor.

```tut:silent
object MyEnum extends Enumeration { val foo, bar = Value }
implicit val MyEnumAtom = pgEnum(MyEnum, "myenum")
```

```tut
sql"select 'foo'::myenum".query[MyEnum.Value].unique.quick.run
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
sql"select 'foo'::myenum".query[FooBar].unique.quick.run
```


### Geometric Types

The following geometric types are supported, and map to driver-supplied types. These will normally be mapped to application-specific types.

- the `box` schema type maps to `org.postgresql.geometric.PGbox`
- the `circle` schema type maps to `org.postgresql.geometric.PGcircle`
- the `lseg` schema type maps to `org.postgresql.geometric.PGlseg`
- the `path` schema type maps to `org.postgresql.geometric.PGpath`
- the `point` schema type maps to `org.postgresql.geometric.PGpoint`
- the `polygon` schema type maps to `org.postgresql.geometric.PGpolygon`

### PostGIS Types

This adds support for the main PostGIS types:

- `PGgeometry`
- `PGbox2d`
- `PGbox3d`

As well as the following abstract and fine-grained types carried by `PGgeometry`:

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

A complete table of SQLSTATE values is provided in the `doobie.contrib.postgresql.sqlstate` module, and recovery combinators for each of these (`onUniqueViolation` for example) are provided in `doobie.contrib.postgresql.syntax`. 


