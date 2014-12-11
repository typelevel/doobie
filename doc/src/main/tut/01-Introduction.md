---
layout: book
number: 1
title: Introduction
---


This is a very short book about **doobie**, which is a pure-functional JDBC layer for Scala. 

Although **doobie** provides low-level access to everything in `java.sql` (JDBC 4.0), allowing you to write any JDBC program in a pure functional style, the focus of this book is the **high-level API**, which is where most users will spend their time.

This book is organized cookbook-style: we demonstrate a common task and then explain how it works, probably going into more detail than you want, but you may return to it later. The goal is to get you up and running quickly, but give you a handle to the deeper stuff when you need it. You're a scala programmer, you can handle it.


#### Target Audience

This library is designed for people who are interested in typed, pure functional programming. If you are not a [scalaz](https://github.com/scalaz/scalaz) user or are not familiar with functional I/O and monadic effects, you may need to go slowly and may want to spend some time reading [Functional Programming in Scala](http://manning.com/bjarnason/), which introduces all of the ideas that you will find when exploring **doobie**.

Having said this, if you find yourself confused or frustrated by this documentation or the **doobie** API, *please* file an [issue](https://github.com/tpolecat/doobie/issues) or find `tpolecat` on Twitter or `#scala` (FreeNode IRC) and ask for help. Both the library and the documentation are young and changing quickly, and it is inevitable that some things will be unclear.


#### Conventions

This manual is compiled as part of the build using the [tut](https://github.com/tpolecat/tut) tutorial generator, so the code examples are guaranteed to compile (and with luck should also work correctly). Each page stands on its own; if you copy and paste stuff starting from the top, it will work in your REPL as long as you have the matching version of **doobie** and the [H2](http://www.h2database.com/) driver, which you can add to your sbt build as follows:

```scala
libraryDependencies ++= "com.h2database" %  "h2" % "1.3.170"
```

Each page begins with some imports, like this.

```tut:silent
import scalaz._, Scalaz._
import doobie.imports._
```

Sometimes definitions will stand alone.

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

#### Feedback and Contributions

Feedback of all kinds (especially negative) on **doobie** or this book is genuinely welcome. Please feel free to file a [pull request](https://github.com/tpolecat/doobie) if you have a contribution, or file an [issue](https://github.com/tpolecat/doobie/issues), or find and chat with `tpolecat` as mentioned above.

```tut
val generatedOn = new java.util.Date
```
