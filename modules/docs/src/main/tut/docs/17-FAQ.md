---
layout: docs
number: 17
title: Frequently-Asked Questions
---

## {{page.title}}

In this chapter we address some frequently-asked questions, in no particular order. First a bit of set-up.

```tut:silent
import cats._
import cats.data._
import cats.effect.IO
import cats.effect.implicits._
import cats.implicits._
import doobie._
import doobie.implicits._
import java.awt.geom.Point2D
import java.util.UUID
import shapeless._
import scala.concurrent.ExecutionContext

// We need a ContextShift[IO] before we can construct a Transactor[IO]. The passed ExecutionContext
// is where nonblocking operations will be executed.
implicit val cs = IO.contextShift(ExecutionContext.global)

// A transactor that gets connections from java.sql.DriverManager and executes blocking operations
// on an unbounded pool of daemon threads. See the chapter on connection handling for more info.
val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", // driver classname
  "jdbc:postgresql:world", // connect URL (driver-specific)
  "postgres",              // user
  ""                       // password
)

val y = xa.yolo
import y._
```

### How do I do an `IN` clause?

This used to be very irritating, but as of 0.4.0 there is a good solution. See the section on `IN` clauses in [Chapter 5](05-Parameterized.html) and [Chapter 8](08-Fragments.html) on statement fragments.

### How do I ascribe an SQL type to an interpolated parameter?

Interpolated parameters are replaced with `?` placeholders, so if you need to ascribe an SQL type you can use vendor-specific syntax in conjunction with the interpolated value. For example, in PostgreSQL you use `:: type`:

```tut
val s = "foo"
sql"select $s".query[String].check.unsafeRunSync
sql"select $s :: char".query[String].check.unsafeRunSync
```

### How do I do several things in the same transaction?

You can use a `for` comprehension to compose any number of `ConnectionIO` programs, and then call `.transact(xa)` on the result. All of the composed programs will run in the same transaction. For this reason it's useful for your APIs to expose values in `ConnectionIO`, so higher-level code can place transaction boundaries as needed.

### How do I run something outside of a transaction?

`Transactor.transact` takes a `ConnectionIO` and constructs an `IO` or similar that will run it in a single transaction, but it is also possible to include transaction boundaries *within* a `ConnectionIO`, and to disable transaction handling altogether. Some kinds of DDL statements may require this for some databases. You can define a combinator to do this for you.

```tut:silent
/**
 * Take a program `p` and return an equivalent one that first commits
 * any ongoing transaction, runs `p` without transaction handling, then
 * starts a new transaction.
 */
def withoutTransaction[A](p: ConnectionIO[A]): ConnectionIO[A] =
  FC.setAutoCommit(true).bracket(_ => p)(_ => FC.setAutoCommit(false))
```

Note that you need both of these operations if you are using a `Transactor` because it will always start a transaction and will try to commit on completion.


### How do I turn an arbitrary SQL string into a `Query0/Update0`?

As of **doobie** 0.4.0 this is done via [statement fragments](08-Fragments.html). Here we choose the sort order dynamically.

```tut:silent
case class Code(country: String)
case class City(code: Code, name: String, population: Int)

def cities(code: Code, asc: Boolean): Query0[City] = {
  val ord = if (asc) fr"ASC" else fr"DESC"
  val sql = fr"""
    SELECT countrycode, name, population
    FROM   city
    WHERE  countrycode = $code
    ORDER BY name""" ++ ord
  sql.query[City]
}
```

We can check the resulting `Query0` as expected.

```tut:plain
cities(Code("USA"), true).check.unsafeRunSync
```

And it works!

```tut
cities(Code("USA"), true).stream.take(5).quick.unsafeRunSync
cities(Code("USA"), false).stream.take(5).quick.unsafeRunSync
```

### How do I handle outer joins?

With an outer join you end up with set of nullable columns, which you typically want to map to a single `Option` of some composite type, which doobie can do for you. If all columns are null you will get back `None`.

```tut:silent
case class Country(name: String, code: String)
case class City(name: String, district: String)

val join =
  sql"""
    select c.name, c.code,
           k.name, k.district
    from country c
    left outer join city k
    on c.capital = k.id
  """.query[(Country, Option[City])]
```

Some examples, filtered for size.

```tut
join.stream.filter(_._1.name.startsWith("United")).quick.unsafeRunSync
```

### How do I log the SQL produced for my query after interpolation?

As of **doobie** 0.4 there is a reasonable solution to the logging/instrumentation question. See [Chapter 10](10-Logging.html) for more details.

### Why is there no `Get` or `Put` for `SQLXML`?

There are a lot of ways to handle `SQLXML` so there is no pre-defined strategy, but here is one that maps `scala.xml.Elem` to `SQLXML` via streaming.

```tut:silent
import doobie.enum.JdbcType.Other
import java.sql.SQLXML
import scala.xml.{ XML, Elem }

implicit val XmlMeta: Meta[Elem] =
  Meta.Advanced.one[Elem](
    Other,
    NonEmptyList.of("xml"),
    (rs, n) => XML.load(rs.getObject(n).asInstanceOf[SQLXML].getBinaryStream),
    (ps, n,  e) => {
      val sqlXml = ps.getConnection.createSQLXML
      val osw = new java.io.OutputStreamWriter(sqlXml.setBinaryStream)
      XML.write(osw, e, "UTF-8", false, null)
      osw.close
      ps.setObject(n, sqlXml)
    },
    (_, _,  _) => sys.error("update not supported, sorry")
  )
```

## How do I set the chunk size for streaming results?

By default streams constructed with the `sql` interpolator are fetched `Query.DefaultChunkSize` rows at a time (currently 512). If you wish to change this chunk size you can use `processWithChunkSize` for queries, and `withGeneratedKeysWithChunkSize` for updates that return results.

## My Postgres domains are all type checking as DISTINCT! How can I get my Yolo tests to pass?

Domains with check constraints will type check as DISTINCT. For Doobie later than 0.4.4, in order to get the type checks to pass, you can define a `Meta` of with target type Distinct and `xmap` that instances. For example,

```tut:silent
import cats.data.NonEmptyList
import doobie._
import doobie.enum.JdbcType.{Distinct => JdbcDistinct, _}

object distinct {
def string(name: String): Meta[String] =
  Meta.advanced(
    NonEmptyList.of(JdbcDistinct, VarChar),
    NonEmptyList.of(name),
    _ getString _,
    _.setString(_, _),
    _.updateString(_, _)
  )

case class NonEmptyString(value: String)

// If the domain for NonEmptyStrings is nes
implicit val nesMeta: Meta[NonEmptyString] =
  string("nes").imap(NonEmptyString.apply)(_.value)
```
