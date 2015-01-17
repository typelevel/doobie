---
layout: book
number: 1
title: Introduction
---


This is a very short book about **doobie**, which is a pure-functional JDBC layer for Scala. 

**doobie** provides low-level access to everything in `java.sql` (as of JDK 1.6, JDBC 4.0), allowing you to write any JDBC program in a pure functional style. However the focus of this book is the **high-level API**, which is where most users will spend their time.

This book is organized cookbook-style: we demonstrate a common task and then explain how it works, perhaps in more detail than you want right now. The goal is to get you up and running quickly, but give you a handle on the deeper stuff if you need it later.


### Target Audience

This library is designed for people who are interested in typed, pure functional programming. If you are not a [scalaz](https://github.com/scalaz/scalaz) user or are not familiar with functional I/O and monadic effects, you may need to go slowly and may want to spend some time reading [Functional Programming in Scala](http://manning.com/bjarnason/), which introduces all of the ideas that you will find when exploring **doobie**.

Having said this, if you find yourself confused or frustrated by this documentation or the **doobie** API, *please* file an [issue](https://github.com/tpolecat/doobie/issues) or find `tpolecat` on Twitter or `#scala` (FreeNode IRC) and ask for help. Both the library and the documentation are young and are changing quickly, and it is inevitable that some things will be unclear.


### The Setup

This book is compiled as part of the build using the [tut](https://github.com/tpolecat/tut) tutorial generator, so the code examples are guaranteed to compile (and with luck should also work correctly). Each page stands on its own; if you copy and paste stuff starting from the top, it will work in your REPL as long as you have the proper version of **doobie** and the [H2](http://www.h2database.com/) driver, which you can add to your sbt build as follows:

```scala
resolvers ++= Seq(
  "tpolecat" at "http://dl.bintray.com/tpolecat/maven",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)

libraryDependencies ++= Seq(
  "org.tpolecat"   %% "doobie-core"       % "0.2.0",
  "org.tpolecat"   %% "doobie-contrib-h2" % "0.2.0",
  "com.h2database" %  "h2"                % "1.3.170"
)
```

### Conventions

Each page begins with some imports, like this.

```tut:silent
import scalaz._, Scalaz._
import doobie.imports._
```

After that there is text interspersed with code examples. Sometimes definitions will stand alone.

```tut:silent

case class Person(name: String, age: Int)

val nel = NonEmptyList(Person("Bob", 12), Person("Alice", 14))
```
And sometimes they will appear as a REPL interaction.

```tut
nel.head
nel.tail
```

Sometimes we demonstrate that something doesn't compile. In such cases it will be clear from the context that this is expected, and not a problem with the documentation.

```tut:nofail
woozle(nel) // doesn't compile
```

### Feedback and Contributions

Feedback of all kinds (especially negative) on **doobie** or this book is genuinely welcome. Please feel free to file a [pull request](https://github.com/tpolecat/doobie) if you have a contribution, or file an [issue](https://github.com/tpolecat/doobie/issues), or find and chat with `tpolecat` as mentioned above.

