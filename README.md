# doobie

[![Travis CI](https://travis-ci.org/tpolecat/doobie.svg?branch=master)](https://travis-ci.org/tpolecat/doobie)
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

Supported releases and dependencies are shown below.

| doobie | status |  jdk | scala            | scalaz | scalaz-stream | shapeless |
|:------:|:------:|:----:|------------------|:------:|:-------------:|:---------:|
|  0.3.0 | stable | 1.8+ | 2.10, 2.11, 2.12 |   7.2  |      0.8      |    2.3    |
|  0.2.4 | stable | 1.7+ | 2.10, 2.11       |   7.1  |      0.8      |    2.2    |
|  0.2.3 |   eol  | 1.6+ | 2.10, 2.11       |   7.1  |      0.7      |    2.2    |

Note that **doobie** is pre-1.0 software and is still undergoing active development. New versions are **not** binary compatible with prior versions, although in most cases user code will be source compatible. Nontrivial breaking changes will be introduced through a deprecation cycle of at least one minor (0.x) release.

To use **doobie** you need to add the following to your `build.sbt`.

```scala
libraryDependencies += "org.tpolecat" %% "doobie-core" % "0.3.0" // or any supported release above
```

If you are using Scala 2.10 you must also add the paradise compiler plugin.

```scala
addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
```

It is likely that you will want one or more add-on libraries. **doobie** provides the following, which have the same version as `doobie-core` and are released together.

* `doobie-contrib-h2` for [H2](http://www.h2database.com/html/main.html)-specific type mappings.
* `doobie-contrib-hikari` for [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pooling.
* `doobie-contrib-postgresql` for [PostgreSQL](http://postgresql.org)-specific type mappings.
* `doobie-contrib-specs2` for [specs2](http://etorreborre.github.io/specs2/) support for typechecking queries.

See the [**book of doobie**](http://tpolecat.github.io/doobie-0.3.0/00-index.html) for more information on these add-ons.

## Development Snapshots

The development version is **0.3.1-SNAPSHOT**. It is updated continuously and without warning, so feel free to experiment but don't depend on it. So far differs from **0.3.0** in at least the following important ways:

- Artifacts are now published for [Cats](http://typelevel.org/cats/)! Artifact names are the same but end in `-cats`, so `doobie-core-cats` and `doobie-h2-cats`. The scalaz and Cats variants are compiled without shims or indirection; **doobie** now uses a preprocessor to make slight adjustments to the source to compile it "natively" for both libraries. See below for more details.
- The `contrib` segment in artifacts and package names is gone. So `doobie-h2` is the artifact now and `doobie.h2` is the package name.
- The `posgresql` segment and package name has been shortened to `postgres`.
- The **book of doobie** now uses `IOLite` (included in `doobie.imports._`) instead of `Task`, which is trivially different between scalaz and fs2 and complicates the yaxing. It's a totally inconsequential change but I think it may freak people out. 

### Cats Support

The `0.3.1-SNAPSHOT` release is [also] compiled for [Cats 0.7.2](http://typelevel.org/cats/) with [FS2 0.9.0](https://github.com/functional-streams-for-scala/fs2) for **2.11 only** (FS2 isn't available for 2.10 and Cats isn't available for 2.12).

Doc links for Cats artifacts (no unidoc yet, sorry):
[core](https://oss.sonatype.org/service/local/repositories/snapshots/archive/org/tpolecat/doobie-core_2.11/0.3.1-SNAPSHOT/doobie-core_2.11-0.3.1-SNAPSHOT-javadoc.jar/!/index.html)
• [h2](https://oss.sonatype.org/service/local/repositories/snapshots/archive/org/tpolecat/doobie-h2_2.11/0.3.1-SNAPSHOT/doobie-h2_2.11-0.3.1-SNAPSHOT-javadoc.jar/!/index.html)
• [hikari](https://oss.sonatype.org/service/local/repositories/snapshots/archive/org/tpolecat/doobie-hikari_2.11/0.3.1-SNAPSHOT/doobie-hikari_2.11-0.3.1-SNAPSHOT-javadoc.jar/!/index.html)
• [postgres](https://oss.sonatype.org/service/local/repositories/snapshots/archive/org/tpolecat/doobie-postgres_2.11/0.3.1-SNAPSHOT/doobie-postgres_2.11-0.3.1-SNAPSHOT-javadoc.jar/!/index.html)
• [book of doobie](http://tpolecat.github.io/doobie-cats-0.3.1-SNAPSHOT/00-index.html)

The obligatory example:

```scala
scala> import doobie.imports._
import doobie.imports._

scala> val xa = DriverManagerTransactor[IOLite]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
xa: doobie.util.transactor.Transactor[doobie.imports.IOLite] = doobie.util.transactor$DriverManagerTransactor$$anon$2@9ab5c78

scala> val c = sql"select name, population from country".query[(String, Int)].list
c: doobie.free.connection.ConnectionIO[List[(String, Int)]] = Gosub(Suspend(PrepareStatement4(select name, population from country)),<function1>)

scala> val c2 = c.map(_.toMap) // lose the type alias so we see it's cats.free.Free!
c2: cats.free.Free[doobie.free.connection.ConnectionOp,scala.collection.immutable.Map[String,Int]] = Gosub(Gosub(Suspend(PrepareStatement4(select name, population from country)),<function1>),<function1>)

scala> c2.transact(xa).unsafePerformIO
res2: scala.collection.immutable.Map[String,Int] = Map(Kazakstan -> 16223000, Gibraltar -> 25000, Haiti -> 8222000, Grenada -> 94000, Vanuatu -> 190000, Iraq -> 23115000, Poland -> 38653600, East Timor -> 885000, Saint Helena -> 6000, Montserrat -> 11000, Martinique -> 395000, Jordan -> 5083000, Gabon -> 1226000, Netherlands Antilles -> 217000, United States Minor Outlying Islands -> 0, Philippines -> 75967000, Somalia -> 10097000, Madagascar -> 15942000, Andorra -> 78000, Falkland Islands -> 2000, Algeria -> 31471000, Liechtenstein -> 32300, Norfolk Island -> 2000, Yugoslavia -> 10640000, Kiribati -> 83000, Angola -> 12878000, Croatia -> 4473000, Luxembourg -> 435700, Lebanon -> 3282000, United States -> 278357000, Greece -> 10545700, Eritrea -> 3850000, Bhuta...
```

It should go without saying, but the appearance of a feature in a pre-release version is not a promise that it will appear in the final release. The `yax` preprocessor (and therefore Cats support) is *very* experimental.


## Documentation and Support

- See the [**changelog**](https://github.com/tpolecat/doobie/blob/series/0.3.x/CHANGELOG.md#0.3.0) for an overview of changes in this and previous versions.
- Behold the [**book of doobie**](http://tpolecat.github.io/doobie-0.3.0/00-index.html) ← start here
- The [**scaladoc**](http://tpolecat.github.io/doc/doobie/0.3.0/api/index.html) will be handy once you get your feet wet.
- There is also the source. If you're here you know where to look. Check the examples.
- If you have comments or run into trouble, please file an issue.
- Find **tpolecat** on the FreeNode `#scala` channel, or join the [**Gitter Channel**](https://gitter.im/tpolecat/doobie).

## Presentations, Blog Posts, etc.

Listed newest first. If you have given a presentation or have written a blog post that includes **doobie**, let me know and I'll add it to this list.

- [The Functional Web Stack](https://t.co/rYH42gs2AU) by Gary Coady - Dublin Scala Users Group, April 2016
- [End to End and On The Level](https://www.youtube.com/watch?v=lMW_yMkxX4Q&list=PL_5uJkfWNxdkQd7FbN1whrTOsJPMgHgLg&index=2) by Dave Gurnell - Typelevel Summit, Philadelphia, March 2016
- [Programs as Values: JDBC Programming with doobie](https://www.youtube.com/watch?v=M5MF6M7FHPo) by Rob Norris - Scala by the Bay, 2015 - [slides](http://tpolecat.github.io/assets/sbtb-slides.pdf)
- [Typechecking SQL in Slick and doobie](http://underscore.io/blog/posts/2015/05/28/typechecking-sql.html) by Richard Dallaway
- [DB to JSON with a Microservice](http://da_terry.bitbucket.org/slides/presentation-scalasyd-functional-jdbc-http/#/) by Da Terry - [code](https://bitbucket.org/da_terry/scalasyd-doobie-http4s)

## Testing

If you want to build and run the tests for yourself, you'll need a local postgresql database. Tests are run as the default **postgres** user, which should have no password for access in the local environment. You can see the `before_script` section of the [.travis.yml](./.travis.yml) file for an up-to-date list of steps for preparing the test database.
