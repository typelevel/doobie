---
layout: docs
number: 12
title: Custom Mappings
---

## {{page.title}}

In this chapter we learn how to use custom `Meta` instances to map arbitrary data types as single-column values; and how to use custom `Composite` instances to map arbitrary types across multiple columns.

### Setting Up

The examples in this chapter require the `doobie-postgres` add-on, as well as the [circe](http://circe.io/) JSON library, which you can add to your build thus:

```scala
val circeVersion = "0.9.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
```

In our REPL we have the same setup as before, plus a few extra imports.

```tut:silent
import cats._
import cats.effect.IO
import cats.implicits._
import doobie._
import doobie.implicits._
import io.circe._
import io.circe.jawn._
import io.circe.syntax._
import java.awt.Point
import org.postgresql.util.PGobject
import scala.reflect.runtime.universe.TypeTag
import scala.util.Try

val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)

val y = xa.yolo
import y._
```

### Meta and Composite

The `doobie.free` API provides constructors for JDBC actions like `setString(1, "foo")` and `getBoolean(4)`, which operate on single columns specified by name or offset. Query parameters are set and resulting rows are read by repeated applications of these low-level actions.

The `doobie.hi` API abstracts the construction of these composite operations via the `Composite` typeclass, which provides actions to get or set a heterogeneous **sequence** of column values. For example, the following programs are equivalent:

```tut:silent
// Using doobie.free
FPS.setString(1, "foo") *> FPS.setInt(2, 42)

// Using doobie.hi
HPS.set(1, ("foo", 42))

// Or leave the 1 out if you like, since we usually start there
HPS.set(("foo", 42))

// Which simply delegates to the Write instance
Write[(String,Int)].set(1, ("foo", 42))
```

**doobie** can derive `Composite` instances for primitive column types and options thereof, plus tuples, `HList`s, shapeless records, and case classes whose elements have `Composite` instances. These primitive column types are identified by `Meta` instances, which describe single-column mappings.

So our strategy for mapping custom types is to construct a new `Meta` instance (given `Meta[A]` you get `Composite[A]` and `Composite[Option[A]]` for free); and our strategy for multi-column mappings is to construct a new `Composite` instance. We consider both cases below.

### Meta by Invariant Map

Let's say we have a structured value that's represented by a single string in a legacy database. We also have conversion methods to and from the legacy format.

```tut:silent
case class PersonId(department: String, number: Int) {
  def toLegacy = department + ":" + number
}

object PersonId {

  def fromLegacy(s: String): Option[PersonId] =
    s.split(":") match {
      case Array(dept, num) => Try(num.toInt).toOption.map(new PersonId(dept, _))
      case _                => None
    }

  def unsafeFromLegacy(s: String): PersonId =
    fromLegacy(s).getOrElse(throw new RuntimeException("Invalid format: " + s))

}

val pid = PersonId.unsafeFromLegacy("sales:42")
```

Because `PersonId` is a case class of primitive column values, we can already map it across two columns. We can look at its `Composite` instance and see that its column span is two:

```tut:nofail
Write[PersonId].length
```

However if we try to use this type for a *single* column value (i.e., as a query parameter, it doesn't compile.

```tut:fail:plain
sql"select * from person where id = $pid"
```

According to the error message we need a `Param[PersonId :: HNil]` instance which requires a `Meta` instance for each member, which means we need a `Meta[PersonId]`.

```tut:fail
Put[PersonId]
```

... and we don't have one. So how do we get one? The simplest way is by basing it on an existing `Meta` instance, using `xmap`. So we simply provide `String => PersonId` and vice-versa and we're good to go.

```tut:silent
implicit val PersonIdMeta: Meta[PersonId] =
  Meta[String].timap(PersonId.unsafeFromLegacy)(_.toLegacy)
```

Now it compiles as a column value and as a `Composite` that maps to a *single* column:

```tut
sql"select * from person where id = $pid"
Write[PersonId].length
sql"select 'podiatry:123'".query[PersonId].quick.unsafeRunSync
```

Note that the `Composite` width is now a single column. The rule is: if there exists an instance `Meta[A]` in scope, it will take precedence over any automatic derivation of `Composite[A]`.

### Meta by Construction

Some modern databases support a `json` column type that can store structured data as a JSON document, along with various SQL extensions to allow querying and selecting arbitrary sub-structures. So an obvious thing we might want to do is provide a mapping from Scala model objects to JSON columns, via some kind of JSON serialization library.

We can construct a `Meta` instance for the circe `Json` type by using the `Meta.other` constructor, which constructs a direct object mapping via JDBC's `.getObject` and `.setObject`. In the case of PostgreSQL the JSON values are marshalled via the `PGObject` type, which encapsulates an uninspiring `(String, String)` pair representing the schema type and its string value.

Here we go:

```tut:silent
implicit val JsonMeta: Meta[Json] =
  Meta.other[PGobject]("json").timap[Json](
    a => parse(a.getValue).leftMap[Json](e => throw e).merge)( // failure raises an exception
    a => {
      val o = new PGobject
      o.setType("json")
      o.setValue(a.noSpaces)
      o
    }
  )
```

Given this mapping to and from `Json` we can construct a *further* mapping to any type that has `Encoder` and `Decoder` instances. On failure we throw an exception; this indicates a logic or schema problem.

```tut:silent
def codecMeta[A : Encoder : Decoder : TypeTag]: Meta[A] =
  Meta[Json].timap[A](
    _.as[A].fold[A](throw _, identity))(
    _.asJson
  )
```

Let's make sure it works. Here is a simple data type with a circe encoder, taken straight from the website, and a `Meta` instance derived from the code above.

```tut:silent
case class Person(name: String, age: Int, things: List[String])

implicit val personEncodeJson =
  Encoder.forProduct3("name", "age", "things") { (p: Person) =>
    (p.name, p.age, p.things)
  }

implicit val personDecodeJson =
  Decoder.forProduct3("name", "age", "things") {
    (name: String, age: Int, things: List[String]) =>
      Person(name, age, things)
  }

implicit val PersonMeta = codecMeta[Person]
```

Now let's create a table that has a `json` column to store a `Person`.

```tut:silent
val drop = sql"DROP TABLE IF EXISTS pet".update.run

val create =
  sql"""
    CREATE TABLE pet (
      id    SERIAL,
      name  VARCHAR NOT NULL UNIQUE,
      owner JSON    NOT NULL
    )
  """.update.run

(drop *> create).quick.unsafeRunSync
```

Note that our `check` output now knows about the `Json` and `Person` mappings. This is a side-effect of constructing instance above, which isn't a good design. Will revisit this for 0.3.0; this information is only used for diagnostics so it's not critical.

```tut:plain
sql"select owner from pet".query[Int].check.unsafeRunSync
```

And we can now use `Person` as a parameter type and as a column type.

```tut
val p = Person("Steve", 10, List("Train", "Ball"))

{
  sql"insert into pet (name, owner) values ('Bob', $p)"
    .update
    .withUniqueGeneratedKeys[(Int, String, Person)]("id", "name", "owner")
    .quick
    .unsafeRunSync
}
```

If we ask for the `owner` column as a string value we can see that it is in fact storing JSON data.

```tut
sql"select name, owner from pet".query[(String,String)].quick.unsafeRunSync
```

### Composite by Invariant Map

We get `Composite[A]` and `Composite[Option[A]]` for free given `Meta[A]`, or for tuples, `HList`s, shapeless records, and case classes whose fields have `Composite` instances. This covers a lot of cases, but we still need a way to map other types. For example, what if we wanted to map a `java.awt.Point` across two columns? Because it's not a tuple or case class we can't do it for free, but we can get there via invariant map. Here we map `Point` to a pair of `Int` columns.

```tut:silent
implicit val Point2DComposite: Read[Point] =
  Read[(Int, Int)].map { case (x, y) => new Point(x, y) }
```

And it works!

```tut
sql"select 'foo', 12, 42, true".query[(String, Point, Boolean)].unique.quick.unsafeRunSync
```
