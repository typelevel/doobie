# doobie

**doobie** is a pure functional JDBC layer for Scala. It is not an ORM, nor is it a relational algebra; it just provides a principled way to construct programs (and higher-level APIs) that use JDBC. **doobie** introduces very few new abstractions; if you are familiar with basic `scalaz` typeclasses like `Functor` and `Monad` you should have no trouble here.

## Quick Start

The current release is **0.2.0**. You should expect breaking changes for at least the next few versions, although these will be documented and kept to a minimum. To use **doobie** you need to add the following to your `build.sbt`.

```scala
resolvers ++= Seq(
  "tpolecat" at "http://dl.bintray.com/tpolecat/maven",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)

libraryDependencies ++= {
  "org.tpolecat" %% "doobie-core" % "0.2.0", 
  "org.tpolecat" %% "doobie-contrib-postgres" % "0.2.0",      // for PostgreSQL-specific types
  "org.tpolecat" %% "doobie-contrib-h2" % "0.2.0",            // for H2-specific types
  "org.tpolecat" %% "doobie-contrib-specs" % "0.2.0" % "test" // support for query checking in Specs2 
}
```

**doobie** **0.2.0** works with **Scala 2.10 and 2.11** and **scalaz 7.1**.

## Documentation and Support

- Behold the [**book of doobie**](http://tpolecat.github.io/doobie-0.2.0-SNAPSHOT/00-index.html) ← start here
- There is also the source. If you're here you know where to look. Check the examples.
- If you have comments or run into trouble, please file an issue.
- Find **tpolecat** on the FreeNode `#scala` channel.




