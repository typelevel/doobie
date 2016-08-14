---
layout: book
number: 15
title: Frequently-Asked Questions
---

In this chapter we address some frequently-asked questions, in no particular order. First a bit of set-up.

```tut:silent
import doobie.imports._
import java.awt.geom.Point2D
import java.util.UUID
import scalaz._, Scalaz._
import scalaz.concurrent.Task
import shapeless._

val xa = DriverManagerTransactor[Task](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)

import xa.yolo._
```

### How do I do an `IN` clause?

This used to be very irritating, but as of 0.2.3 is only moderately irritating. See the section on `IN` clauses in [Chapter 5](05-Parameterized.html).

### How do I ascribe a SQL type to an interpolated parameter?

Interpolated parameters are replaced with `?` placeholders, so if you need to ascribe a SQL type you can use vendor-specific syntax in conjunction with the interpolated value. For example, in PostgreSQL you use `:: type`:

```tut
val s = "foo"
sql"select $s".query[String].check.run
sql"select $s :: char".query[String].check.run
```

### How do I do several things in the same transaction?

You can use a `for` comprehension to compose any number of `ConnectionIO` programs, and then call `.transact(xa)` on the result. All of the composed programs will run in the same transaction. For this reason it's useful for your APIs to expose values in `ConnectionIO`, so higher-level code can place transaction boundaries as needed. 

### How do I turn an arbitrary SQL string into a `Query/Query0`?

The `sql` interpolator does not allow arbitrary string interpolation in SQL literals; each interpolated value becomes a `?` placeholder, paired with a type-appropriate `setXXX` action. So if you wish to generate SQL statements dynamically you cannot use the `sql` interpolator. Instead construct the SQL literal with placeholders for parameters, and pass this to the `Query` constructor. You can then apply your parameters (tupled if there are several) to produce the desired `Query0`.

```tut:silent
case class Code(country: String)
case class City(code: Code, name: String, population: Int)

def cities(code: Code, asc: Boolean): Query0[City] = {
  val sql = s"""
    SELECT countrycode, name, population
    FROM   city
    WHERE  countrycode = ?
    ORDER BY name ${if (asc) "ASC" else "DESC"}
  """
  Query[Code, City](sql, None).toQuery0(code)
}
```

We can check the resulting `Query0` as expected.

```tut:plain
cities(Code("USA"), true).check.run
```

And it works!

```tut
cities(Code("USA"), true).process.take(5).quick.run
cities(Code("USA"), false).process.take(5).quick.run
```

### How do I handle outer joins?

With an outer join you end up with set of nullable columns, which you typically want to map to a single `Option` of some composite type. The most straightforward way do this is to select the `Option` columns directly, then use the `map` method on `Query0` to transform the result type using applicative composition on the optional values:

```tut:silent
case class Country(name: String, code: String)
case class City(name: String, district: String)

val join: Query0[(Country, Option[City])] = 
  sql"""
    select c.name, c.code,
           k.name, k.district 
    from country c 
    left outer join city k 
    on c.capital = k.id
  """.query[(Country, Option[String], Option[String])].map {
    case (c, n, d) => (c, (n |@| d)(City))
  }
```

Some examples, filtered for size.

```tut
join.process.filter(_._1.name.startsWith("United")).quick.run
```

### How do I resolve `error: Could not find or construct Param[...]`?

When we use the `sql` interpolator we require a `Param` instance for an `HList` composed of the types of the interpolated query parameters. For instance, in the following code (which has parameters of type `String` and `UUID`, in that order) we need a `Param[String :: UUID :: HNil]` and none is available.

```tut:fail:plain
def query(s: String, u: UUID) = sql"… $s … $u …".query[Int]
```

Ok, so the message suggests that we need an `Atom` instance for each type in the `HList`, so let's see which one is missing by trying to summon them in the REPL.

```tut:nofail:plain
Atom[String]
Atom[UUID]
```

Ok so we see that there is no `Atom[UUID]`, and as suggested we check to see if there is a `Meta` instance, which there isn't.

```tut:nofail:plain
Meta[UUID]
```

So what this means is that we have not defined a mapping for the `UUID` type to an underlying JDBC type, and **doobie** doesn't know how to set an argument of that type on the underlying `PreparedStatement`. So we have a few choices. We can `nxmap` from an existing `Meta` instance, as described in [Chapter 10](10-Custom-Mappings.html); or we can import a provided mapping from a vendor-specific `contrib` package. Since we're using PostgreSQL here, let's do that.

```tut
import doobie.contrib.postgresql.pgtypes.UuidType
```

Having done this, the `Meta`, `Atom`, and `Param` instances are now present and our code compiles.

```tut
Meta[UUID]
Atom[UUID]
Param[String :: UUID :: HNil]
def query(s: String, u: UUID) = sql"select ... where foo = $s and url = $u".query[Int]
```

### How do I resolve `error: Could not find or construct Composite[...]`?

When we use the `sql` interpolator and use the `.query[A]` method we require a `Composite` instance for the output type `A`, which we can define directly (as described in [Chapter 10](10-Custom-Mappings.html)) or derive automatically if `A` is has an `Atom` instance, or is a product type whose elements have `Composite` instances.

```tut:silent
case class Point(lat: Double, lon: Double)
case class City(name: String, loc: Point)
case class State(name: String, capitol: City)
```

In this case if we were to say `.query[State]` the derivation would be automatic, because all elements of the "flattened" structure have `Atom` instances for free.

```scala
State(String, City(String, Point(Double, Double))) // our structure
     (String,     (String,      (Double, Double))) // is isomorphic to this
      String,      String,       Double, Double    // so we expect a column vector of this shape
```

But what if we wanted to use AWT's `Point2D.Double` instead of our own `Point` class?

```tut:silent
case class City(name: String, loc: Point2D.Double)
case class State(name: String, capitol: City)
```

The derivation now fails.

```tut:fail:plain
sql"…".query[State]
```

And if we look at the flat stucture it's clear that the culprit has to be `Point2D.Double` since we know `String` has a defined column mapping.

```scala
State(String, City(String, Point2D.Double)) // our structure
     (String,     (String, Point2D.Double)) // is isomorphic to this
      String,      String, Point2D.Double   // so we expect a column vector of this shape
```

And indeed this type has no column vector mapping.

```tut:fail:plain
Composite[Point2D.Double]
```

If this were an atomic type it would be a matter of importing or defining a `Meta` instance, but here we need to define a `Composite` directly because we're mapping a type with several members. As this type is isomorphic to `(Double, Double)` we can simply base our mapping off of the existing `Composite`.

```tut:silent
implicit val Point2DComposite: Composite[Point2D.Double] = 
  Composite[(Double, Double)].xmap(
    (t: (Double, Double)) => new Point2D.Double(t._1, t._2),
    (p: Point2D.Double) => (p.x, p.y)
  )
```

Our derivation now works and the code compiles.

```tut
sql"…".query[State]
```

### How do I time query execution?

Because the actual execution of a query does not occur at the site in the code where the query is defined, logging timing is a little challenging. Rob wrote up an example of how to do it in [Issue #267](https://github.com/tpolecat/doobie/issues/267). You'll need to copy the `import`s and the `Combinators` trait into your code base. See the `wickland` object for an example of how to use the `Combinators.timed`.

### How do I log the SQL produced for my query after interpolation?

The result of `sql"…".query` is a `Query0`. The `Query0` type has an `sql` method which provides the SQL. Unfortunately, the SQL includes `?` placeholders for the interpolated values in order to facilitate creating a prepared statement. If you want the values that were interpolated to appear in your logging, you'll have to separately include them in your logging statement.

