---
layout: book
number: 10
title: Custom Mappings
---

### Setting Up

```tut:silent
import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task, java.awt.geom.Point2D
val xa = DriverManagerTransactor[Task](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
import xa.yolo._
```

### Meta, Atom, and Composite

### Meta by Invariant Map

Let's say we have a class that we want to map to a column.

```tut:silent
case class PersonId(toInt: Int)
val pid = PersonId(42)
```

If we try to use this type for a column value, it doesn't compile. However it gives a useful error message.

```tut:nofail
sql"select * from person where id = $pid"
```

So how do we create a `Meta[PersonId]` instance? The simplest way is by basing it on an existing instance, using the invariant functor method `xmap`.

```tut:silent
implicit val PersonIdMeta: Meta[PersonId] = 
  Meta[Int].xmap(PersonId, _.toInt)
```

Now it works!

```tut
sql"select * from person where id = $pid"
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
      owner JSON NOT NULL
    )
  """.update.run

(drop *> create).quick.run
```

Mapping for `Json`, here throwing a `RuntimeException` with the parse error on failure.

```tut:silent
import scala.reflect.runtime.universe.TypeTag
import org.postgresql.util.PGobject

implicit val JsonMeta = Meta.other[PGobject]("json").xmap[Json](
    o => Option(o).map(a => Parse.parse(a.getValue).leftMap[Json](sys.error).merge).orNull,
    a => Option(a).map(a => new PGobject <| (_.setType("json")) <| (_.setValue(a.nospaces))).orNull)

def codecMeta[A: CodecJson: TypeTag] =
  Meta[Json].xmap[A](_.as[A].toOption.get, _.asJson)

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
sql"select x, y from points".query[Point2D.Double]
```

```tut:silent
implicit val Point2DComposite: Composite[Point2D.Double] = 
  Composite[(Double, Double)].xmap(
    t => new Point2D.Double(t._1, t._2),
    p => (p.x, p.y)
  )
```

```tut
sql"select 12, 42".query[Point2D.Double].unique.quick.run
```

