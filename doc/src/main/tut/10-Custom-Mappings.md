---
layout: book
number: 10
title: Custom Mappings
---

In this chapter we examine using custom `Meta` instances to provide new column mappings, and `Composite` mappings to provide new multi-column mappings.

### Setting Up

We will use a very basic setup for this chapter.

```tut:silent
import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task, java.awt.Point
val xa = DriverManagerTransactor[Task](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
import xa.yolo._
```

### Meta, Atom, and Composite

The `doobie.free` API provides constructors for JDBC actions like `setString(1, "foo")` and `getBoolean(4)`, which operate on single columns specified by name or offset. Query parameters are set and resulting rows are read by repeated applications of these low-level actions.

The `doobie.hi` API abstracts the construction of these composite operations via the `Composite` typeclass, which provides actions to get or set a heterogeneous **sequence** of column values. For example, the following programs are equivalent:

```tut:silent
// Using doobie.free
FPS.setString(1, "foo") >> FPS.setInt(2, 42)

// Using doobie.hi
HPS.set(1, ("foo", 42))

// Or leave the 1 out if you like, since we usually start there
HPS.set(("foo", 42))

// Which simply delegates to the Composite instance
Composite[(String,Int)].set(1, ("foo", 42))
```

**doobie** can derive `Composite` instances for primitive column types, plus tuples and case classes whose elements have `Composite` instances. These primitive column types are identified by `Atom` instances, which describe `null`-safe column mappings. These `Atom` instances are almost always derived from lower-level `null`-unsafe mappings specified by the `Meta` typeclass.

So our strategy for mapping custom types is to construct a new `Meta` instance (given `Meta[A]` you get `Atom[A]` and `Atom[Option[A]]` for free); and our strategy for multi-column mappings is to construct a new `Composite` instance. We consider both case below.

### Meta by Invariant Map

Let's say we have a structured value that's represented by a single string in a legacy database. We also have conversion methods to and from the legacy format. 

```tut:silent
case class PersonId(department: String, number: Int) {
  def toLegacy = department + ":" + number
}
object PersonId {

  def fromLegacy(s: String): Option[PersonId] =
    s.split(":") match {
      case Array(dept, num) => num.parseInt.toOption.map(new PersonId(dept, _))
      case _                => None
    }

  def unsafeFromLegacy(s: String): PersonId =
    fromLegacy(s).getOrElse(throw new RuntimeException("Invalid format: " + s))

}
val pid = PersonId.unsafeFromLegacy("sales:42")
```

Because `PersonId` is a case class of primitive column values, we can already map it across two columns. We can look at its `Composite` instance and see that its column span is two:

```tut:nofail
Composite[PersonId].length
```

However if we try to use this type for a *single* column value (i.e., as a query parameter, which requires an `Atom` instance), it doesn't compile.

```tut:nofail
sql"select * from person where id = $pid"
```

According to the error message we need a `Meta[PersonId]` instance. So how do we get one? The simplest way is by basing it on an existing instance, using the invariant functor method `xmap`.

```tut:silent
implicit val PersonIdMeta: Meta[PersonId] = 
  Meta[String].xmap(PersonId.unsafeFromLegacy, _.toLegacy)
```

Now it works as a column value and as a `Composite` that maps to a *single* column:

```tut
sql"select * from person where id = $pid"
sql"select 'podiatry:123'".query[PersonId].quick.run
```

### Meta by Construction

The following example uses the [argonaut](http://argonaut.io/) JSON library, which you can add to your `build.sbt` as follows:

```scala
libraryDependencies += "io.argonaut" %% "argonaut" % "6.1-M4" // as of date of publication
```

Some modern databases support a `json` column type that can store structured data as a JSON document, along with various SQL extensions to allow querying and selecting arbitrary sub-structures. So an obvious thing we might want to do is provide a mapping from Scala model objects to JSON columns.

Here is a simple class with an argonaut serializer, taken straight from the website.

```tut:silent
import argonaut._, Argonaut._

case class Person(name: String, age: Int, things: List[String])
 
implicit def PersonCodecJson =
  casecodec3(Person.apply, Person.unapply)("name", "age", "things")
```

Now let's create a table that has a `json` column.

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

(drop *> create).quick.run
```

Mapping for `Json`, here throwing a `RuntimeException` with the parse error on failure.

```tut:silent
import scala.reflect.runtime.universe.TypeTag
import org.postgresql.util.PGobject

implicit val JsonMeta = Meta.other[PGobject]("json").nxmap[Json](
    a => Parse.parse(a.getValue).leftMap[Json](sys.error).merge,
    a => new PGobject <| (_.setType("json")) <| (_.setValue(a.nospaces)))

def codecMeta[A >: Null : CodecJson: TypeTag] =
  Meta[Json].nxmap[A](_.as[A].toOption.get, _.asJson)

implicit val codecPerson = codecMeta[Person]
```

Note that our `check` output now knows about the `Json` and `Person` mappings. This is a side-effect of constructing instance above, which isn't a good design. Will revisit this, possibly after 0.2.0; this information is only used for diagnostics so it's not critical.

```tut:plain
sql"select owner from pet".query[Int].check.run
```

And we can now use `Person` as a parameter type and as a column type.

```tut

val p = Person("Steve", 10, List("Train", "Ball"))

(sql"insert into pet (name, owner) values ('Bob', $p)"
  .update.withUniqueGeneratedKeys[(Int, String, Person)]("id", "name", "owner")).quick.run

```


### Composite by Invariant Map

We get `Composite[A]` for free given `Atom[A]`, or for tuples and case classes whose fields have `Composite` instances. This covers a lot of cases, but we still need a way to map other types (non-`case` classes for instance). 


```tut:nofail
sql"select x, y from points".query[Point]
```

```tut:silent
val Point2DComposite: Composite[Point] = 
  Composite[(Int, Int)].xmap(
    (t: (Int,Int)) => new Point(t._1, t._2),
    (p: Point) => (p.x, p.y)
  )
```

```tut
// sql"select 12, 42".query[Point].unique.quick.run
```

