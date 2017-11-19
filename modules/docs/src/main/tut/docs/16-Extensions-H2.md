---
layout: docs
number: 16
title: Extensions for H2
---

## {{page.title}}

In this chapter we discuss the extended support that **doobie** offers for users of [H2](http://www.h2database.com/html/main.html) . To use these extensions you must add an additional dependency to your project:

```scala
libraryDependencies += "org.tpolecat" %% "doobie-h2" % doobieVersion
```

This library pulls in [H2 Version 1.3.170](http://www.h2database.com/html/main.html) as a transitive dependency.

### Array Types

**doobie** supports H2 arrays of the following types:

- `Boolean`
- `Int`
- `Long`   
- `Float`  
- `Double`
- `String`

In addition to `Array` you can also map to `List` and `Vector`.

See the previous chapter on **SQL Arrays** for usage examples.

### Other Nonstandard Types

- The `uuid` type is supported and maps to `java.util.UUID`.

### H2 Connection Pool

**doobie** provides a `Transactor` that wraps the connection pool provided by H2. Because the transactor has internal state, constructing one is a side-effect that must be captured (here by `IO`).

```tut:silent
import cats.effect.IO
import doobie._, doobie.implicits._
import doobie.h2._, doobie.h2.implicits._

val q = sql"select 42".query[Int].unique

for {
  xa <- H2Transactor.newH2Transactor[IO]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
  _  <- xa.setMaxConnections(10) // and other ops; see scaladoc or source
  a  <- q.transact(xa)
} yield a
```
