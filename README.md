# doobie

[![Travis CI](https://travis-ci.org/tpolecat/doobie.svg?branch=series/0.3.x)](https://travis-ci.org/tpolecat/doobie)
[![Join the chat at https://gitter.im/tpolecat/doobie](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/tpolecat/doobie?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://img.shields.io/maven-central/v/org.tpolecat/doobie-core_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/org.tpolecat/doobie-core_2.11)

**doobie** is a pure functional JDBC layer for Scala. It is not an ORM, nor is it a relational algebra; it just provides a principled way to construct programs (and higher-level libraries) that use JDBC. **doobie** introduces very few new abstractions; if you are familiar with basic `scalaz` typeclasses like `Functor` and `Monad` you should have no trouble here.

For common use cases **doobie** provides a minimal but expressive high-level API:

```scala
import doobie.imports._, scalaz.effect.IO

val xa = DriverManagerTransactor[IO](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)

case class Country(code: String, name: String, population: Long)

def find(n: String): ConnectionIO[Option[Country]] = 
  sql"select code, name, population from country where name = $n".query[Country].option

// And then

scala> find("France").transact(xa).unsafePerformIO
res0: Option[Country] = Some(Country(FRA,France,59225700))
```

**doobie** is a [Typelevel](http://typelevel.org/) project. This means we embrace pure, typeful, functional programming, and provide a safe and friendly environment for teaching, learning, and contributing as described in the Typelevel [Code of Conduct](http://typelevel.org/conduct.html).

## Quick Start

This branch is available as **0.3.0-SNAPSHOT**, which works on Scala **2.10.5**, **2.11**, and **2.12.0-M3** with

- scalaz 7.2
- scalaz-stream 0.8a
- shapeless 2.3

You should expect minor breaking changes for at least the next few versions, although these will be documented and kept to a minimum. To use **doobie** you need to add the following to your `build.sbt`.

```scala
libraryDependencies += "org.tpolecat" %% "doobie-core" % "0.3.0-SNAPSHOT"
```

If you are using Scala 2.10.5 you must also add the paradise compiler plugin.

```scala
addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
```

It is likely that you will want one or more add-on libraries. **doobie** provides the following, which have the same version as `doobie-core` and are released together.

* `doobie-contrib-h2` for [H2](http://www.h2database.com/html/main.html)-specific type mappings.
* `doobie-contrib-hikari` for [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pooling.
* `doobie-contrib-postgresql` for [PostgreSQL](http://postgresql.org)-specific type mappings.
* `doobie-contrib-specs2` for [specs2](http://etorreborre.github.io/specs2/) support for typechecking queries.

See the [**book of doobie**](http://tpolecat.github.io/doobie-0.2.3/00-index.html) for more information on these add-ons.

## Documentation and Support

- See the [**changelog**](https://github.com/tpolecat/doobie/blob/master/CHANGELOG.md#0.2.3) for an overview of changes in this and previous versions.
- Behold the [**book of doobie**](http://tpolecat.github.io/doobie-0.2.3/00-index.html) ← start here
- The [**scaladoc**](http://tpolecat.github.io/doc/doobie/0.2.3/api/index.html) will be handy once you get your feet wet.
- There is also the source. If you're here you know where to look. Check the examples.
- If you have comments or run into trouble, please file an issue.
- Find **tpolecat** on the FreeNode `#scala` channel, or join the [**Gitter Channel**](https://gitter.im/tpolecat/doobie).

## Presentations, Blog Posts, etc.

Listed newest first. If you have given a presentation or have written a blog post on **doobie**, let me know and I'll add it to this list.

- [Programs as Values: JDBC Programming with doobie](https://www.youtube.com/watch?v=M5MF6M7FHPo) Scala by the Bay, 2015 ([slides](http://tpolecat.github.io/assets/sbtb-slides.pdf))
- [Typechecking SQL in Slick and doobie](http://underscore.io/blog/posts/2015/05/28/typechecking-sql.html) by Richard Dallaway
- [DB to JSON with a Microservice](http://da_terry.bitbucket.org/slides/presentation-scalasyd-functional-jdbc-http/#/) by Da Terry (code [here](https://bitbucket.org/da_terry/scalasyd-doobie-http4s)).



