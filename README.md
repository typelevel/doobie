# doobie

<img align="right" src="https://cdn.rawgit.com/tpolecat/doobie/series/0.4.x/doobie_logo.svg" height="150px" style="padding-left: 20px"/>
[![Travis CI](https://travis-ci.org/tpolecat/doobie.svg?branch=series%2F0.4.x)](https://travis-ci.org/tpolecat/doobie)
[![Join the chat at https://gitter.im/tpolecat/doobie](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/tpolecat/doobie?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://img.shields.io/maven-central/v/org.tpolecat/doobie-core_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/org.tpolecat/doobie-core_2.12)

**doobie** is a pure functional JDBC layer for Scala. It is not an ORM, nor is it a relational algebra; it just provides a principled way to construct programs (and higher-level libraries) that use JDBC. **doobie** introduces very few new abstractions; if you are familiar with core typeclasses like `Functor` and `Monad` you should have no trouble here.

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

Recent releases and dependencies are shown below. The current release is **0.4.0** … if you wish to use an older version please switch to the associated tag. The remainder of this document assumes you're using the current release.

| doobie | status  |  jdk | scala            | scalaz | scalaz-stream | cats | fs2 | shapeless |
|:------:|:-------:|:----:|------------------|:------:|:-------------:|:----:|:---:|:---------:|
|  0.4.0 | current | 1.8+ | 2.10, 2.11, 2.12 |   7.2  |      0.8      | 0.8  | 0.9 |    2.3    |
|  0.3.0 | eol     | 1.8+ | 2.10, 2.11, 2.12 |   7.2  |      0.8      | --   | --  |    2.3    |
|  0.2.4 | eol     | 1.7+ | 2.10, 2.11       |   7.1  |      0.8      | --   | --  |    2.2    |
|  0.2.3 | eol     | 1.6+ | 2.10, 2.11       |   7.1  |      0.7      | --   | --  |    2.2    |

To use **doobie** you need to add **one of the following** to your `build.sbt`.

```scala
libraryDependencies += "org.tpolecat" %% "doobie-core"      % "0.4.0" // scalaz + scalaz-stream
                       "org.tpolecat" %% "doobie-core-cats" % "0.4.0" // cats   + fs2
```

If you are using Scala 2.10 you must also add the paradise compiler plugin.

```scala
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
```

It is likely that you will want one or more add-on libraries. **doobie** provides the following, which have the same version as `doobie-core[-cats]` and are released together.

|  scalaz            |  cats                   | description
|--------------------|-------------------------|-----------------
| `doobie-h2`        | `doobie-h2-cats`        | [H2](http://www.h2database.com/html/main.html)-specific type mappings.
| `doobie-hikari`    | `doobie-hikari-cats`    | [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pooling.
| `doobie-postgres`  | `doobie-postgres-cats`  | [PostgreSQL](http://postgresql.org)-specific type mappings.
| `doobie-specs2`    | `doobie-specs2-cats`    | [specs2](http://etorreborre.github.io/specs2/) support for typechecking queries.
| `doobie-scalatest` | `doobie-scalatest-cats` | [ScalaTest](http://www.scalatest.org/) support for typechecking queries.

See the [**book of doobie**](http://tpolecat.github.io/doobie-0.4.0/00-index.html) for more information on these add-ons.

Note that **doobie** is pre-1.0 software and is still undergoing active development. New versions are **not** binary compatible with prior versions, although in most cases user code will be source compatible.

## Documentation and Support

- See the [**changelog**](https://github.com/tpolecat/doobie/blob/series/0.4.x/CHANGELOG.md#0.4.0) for an overview of changes in this and previous versions.
- Behold the [**book of doobie**](http://tpolecat.github.io/doobie-0.4.0/00-index.html) ← start here
- The [**scaladoc**](http://tpolecat.github.io/doc/doobie/0.4.0/api/index.html) will be handy once you get your feet wet.
- There is also the source. If you're here you know where to look. Check the examples.
- If you have comments or run into trouble, please file an issue.
- Find **tpolecat** on the FreeNode `#scala` channel, or join the [**Gitter Channel**](https://gitter.im/tpolecat/doobie).

## Presentations, Blog Posts, etc.

Listed newest first. If you have given a presentation or have written a blog post that includes **doobie**, let me know and I'll add it to this list.

- [Doobie - Feedback from the Trenches](http://fr.slideshare.net/normation/doobie-feedbacks-from-the-trenches-scalaio-2016) by François Armand, ScalaIO, October 2016
- [Pure Functional Database Programming with Fixpoint Types](https://www.youtube.com/watch?v=7xSfLPD6tiQ) by Rob Norris - Scala World, 2016 - [slides](http://tpolecat.github.io/presentations/sw2016/slides.html#1)
- [The Functional Web Stack](https://t.co/rYH42gs2AU) by Gary Coady - Dublin Scala Users Group, April 2016
- [End to End and On The Level](https://www.youtube.com/watch?v=lMW_yMkxX4Q&list=PL_5uJkfWNxdkQd7FbN1whrTOsJPMgHgLg&index=2) by Dave Gurnell - Typelevel Summit, Philadelphia, March 2016
- [Programs as Values: JDBC Programming with doobie](https://www.youtube.com/watch?v=M5MF6M7FHPo) by Rob Norris - Scala by the Bay, 2015 - [slides](http://tpolecat.github.io/assets/sbtb-slides.pdf)
- [Typechecking SQL in Slick and doobie](http://underscore.io/blog/posts/2015/05/28/typechecking-sql.html) by Richard Dallaway
- [DB to JSON with a Microservice](http://da_terry.bitbucket.org/slides/presentation-scalasyd-functional-jdbc-http/#/) by Da Terry - [code](https://bitbucket.org/da_terry/scalasyd-doobie-http4s)

## Testing

If you want to build and run the tests for yourself, you'll need a local postgresql database. Tests are run as the default **postgres** user, which should have no password for access in the local environment. You can see the `before_script` section of the [.travis.yml](./.travis.yml) file for an up-to-date list of steps for preparing the test database.
