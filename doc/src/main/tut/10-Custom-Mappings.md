---
layout: book
number: 10
title: Custom Mappings
---

In this chapter we learn how to use custom `Meta` instances to map arbitrary data types as single-column values; and how to use custom `Composite` instances to map arbitrary types across multiple columns.

### Setting Up

The examples in this chapter require the `contrib-postgresql` add-on, as well as the [argonaut](http://argonaut.io/) JSON library, which you can to your build thus:

```scala
libraryDependencies += "io.argonaut" %% "argonaut" % "6.1-M4" // as of date of publication
```

In our REPL we have the same setup as before, plus a few extra imports.

```tut:silent
import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task, java.awt.Point
val xa = DriverManagerTransactor[Task](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
import xa.yolo._
import argonaut._, Argonaut._
import scala.reflect.runtime.universe.TypeTag
import org.postgresql.util.PGobject
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

According to the error message we need a `Meta[PersonId]` instance. So how do we get one? The simplest way is by basing it on an existing instance, using `nxmap`, which is like the invariant functor `xmap` but ensures that `null` values are never observed. So we simply provide `String => PersonId` and vice-versa and we're good to go.

```tut:silent
implicit val PersonIdMeta: Meta[PersonId] = 
  Meta[String].nxmap(PersonId.unsafeFromLegacy, _.toLegacy)
```

Now it compiles as a column value and as a `Composite` that maps to a *single* column:

```tut
sql"select * from person where id = $pid"
Composite[PersonId].length
sql"select 'podiatry:123'".query[PersonId].quick.run
```

Note that the `Composite` width is now a single column. The rule is: if there exists an instance `Meta[A]` in scope, it will take precedence over any automatic derivation of `Composite[A]`.

### Meta by Construction - Enumerated Types

SQL enum values are just strings as far as JDBC is concerned, so it's straightforward to say `Meta[String].nxmap(...)` to create a mapping for a Scala-side enumerated type following the pattern discussed above. If you define enumerated types as a `sealed trait` with a number of `case object`s then you will need to use the `nxmap` method. However if you happen to use `scala.Enumeration` you can use the `Meta.enum` constructor.

Let's create an enum (syntax will vary if you're not using PostgreSQL).

```tut:silent
val drop = sql"DROP TYPE IF EXISTS weekday".update.quick
val create = {
  sql"CREATE TYPE weekday AS ENUM ('Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat')"
    .update.quick
}
```

And set it up.

```tut
(drop *> create).run
```

On the Scala side we can create a matching `Enumeration` and construct a mapping via `Meta.enum`.

```tut:silent
object Weekdays extends Enumeration { val Sun, Mon, Tue, Wed, Thu, Fri, Sat = Value }
implicit val WeekdayMeta = Meta.enum(Weekdays)
```

Testing it out we see that it works just fine.

```tut
sql"select 'Mon'::weekday".query[Weekdays.Value].unique.transact(xa).run
```

This also works for enumerated types defined in Java, via the `Meta.javeEnum` constructor. Here we create a mapping for the JDK's `Thread.State` values.

```tut:silent
implicit val ThreadStateMeta = Meta.javaEnum[Thread.State]
```

And it works.

```tut
sql"select 'WAITING'".query[Thread.State].unique.transact(xa).run
```

### Meta by Construction - JSON Encoding

Some modern databases support a `json` column type that can store structured data as a JSON document, along with various SQL extensions to allow querying and selecting arbitrary sub-structures. So an obvious thing we might want to do is provide a mapping from Scala model objects to JSON columns, via some kind of JSON serialization library.

We can construct a `Meta` instance for the argonaut `Json` type by using the `Meta.other` constructor, which constructs a direct object mapping via JDBC's `.getObject` and `.setObject`. In the case of PostgreSQL the JSON values are marshalled via the `PGObject` type, which encapsulates an uninspiring `(String, String)` pair representing the schema type and its string value. 

Here we go:

```tut:silent
implicit val JsonMeta: Meta[Json] = 
  Meta.other[PGobject]("json").nxmap[Json](
    a => Parse.parse(a.getValue).leftMap[Json](sys.error).merge, // failure raises an exception
    a => new PGobject <| (_.setType("json")) <| (_.setValue(a.nospaces))
  )
```

Given this mapping to and from `Json` we can construct a *further* mapping to any type that has a `CodecJson` instance. The `nxmap` constrains us to reference types and requires a `TypeTag` for diagnostics, so the full type constraint is `A >: Null : CodecJson: TypeTag`. On failure we throw an exception; this indicates a logic or schema problem.

```tut:silent
def codecMeta[A >: Null : CodecJson: TypeTag]: Meta[A] =
  Meta[Json].nxmap[A](
    _.as[A].result.fold(p => sys.error(p._1), identity), 
    _.asJson
  )
```

Let's make sure it works. Here is a simple data type with an argonaut serializer, taken straight from the website, and a `Meta` instance derived from the code above.

```tut:silent
case class Person(name: String, age: Int, things: List[String])
 
implicit def PersonCodecJson =
  casecodec3(Person.apply, Person.unapply)("name", "age", "things")

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

(drop *> create).quick.run
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

If we ask for the `owner` column as a string value we can see that it is in fact storing JSON data.

```tut
sql"select name, owner from pet".query[(String,String)].quick.run
```

### Composite by Invariant Map

We get `Composite[A]` for free given `Atom[A]`, or for tuples and case classes whose fields have `Composite` instances. This covers a lot of cases, but we still need a way to map other types. For example, what if we wanted to map a `java.awt.Point` across two columns? Because it's not a tuple or case class we can't do it for free, but we can get there via `xmap`. Here we map `Point` to a pair of `Int` columns.

```tut:silent
implicit val Point2DComposite: Composite[Point] = 
  Composite[(Int, Int)].xmap(
    (t: (Int,Int)) => new Point(t._1, t._2),
    (p: Point) => (p.x, p.y)
  )
```

And it works!

```tut
sql"select 'foo', 12, 42, true".query[(String, Point, Boolean)].unique.quick.run
```


