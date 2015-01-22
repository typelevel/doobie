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

