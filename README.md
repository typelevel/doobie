# doobie

<img src="https://api.travis-ci.org/tpolecat/doobie.svg?branch=master"/><br>
[![Join the chat at https://gitter.im/tpolecat/doobie](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/tpolecat/doobie?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

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

## Quick Start

The current release is **0.2.0**. You should expect breaking changes for at least the next few versions, although these will be documented and kept to a minimum. To use **doobie** you need to add the following to your `build.sbt`.

```scala
resolvers ++= Seq(
  "tpolecat" at "http://dl.bintray.com/tpolecat/maven",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)

libraryDependencies += "org.tpolecat" %% "doobie-core" % "0.2.0"
```

It is likely that you will want one or more add-on libraries. **doobie** provides the following, which have the same version as `doobie-core` and are released together.

* `doobie-h2` for [H2](http://www.h2database.com/html/main.html)-specific type mappings.
* `doobie-hikari` for [HikariCP](https://github.com/brettwooldridge/HikariCP) connection pooling.
* `doobie-postgresql` for [PostgreSQL](postgresql.org)-specific type mappings.
* `doobie-specs2` for [specs2](http://etorreborre.github.io/specs2/) support for typechecking queries.

See the [**book of doobie**](http://tpolecat.github.io/doobie-0.2.0/00-index.html) for [somewhat] more information on these add-ons.

**doobie** **0.2.0** works with **Scala 2.10 and 2.11** and **scalaz 7.1**.

## Documentation and Support

- Behold the [**book of doobie**](http://tpolecat.github.io/doobie-0.2.0/00-index.html) ← start here
- The [**scaladoc**](http://tpolecat.github.io/doc/doobie/0.2.0/api/index.html) will be handy once you get your feet wet.
- There is also the source. If you're here you know where to look. Check the examples.
- If you have comments or run into trouble, please file an issue.
- Find **tpolecat** on the FreeNode `#scala` channel, or join the [Gitter Channel](https://gitter.im/tpolecat/doobie).




