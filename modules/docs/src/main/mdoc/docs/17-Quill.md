## Quill Integration

As of version 0.7 doobie now lets us use [Quill](https://getquill.io) quotes to construct `ConnectionIO` programs. Quill provides statement construction and type mapping, and doobie takes care of statement execution.

In order to use this feature we need to add the following dependency, which pulls in `quill-jdbc` $quillVersion$ transitively.

@@@ vars
```scala
libraryDependencies += "org.tpolecat" %% "doobie-quill" % "$version$"
```
@@@

The examples below require the following imports.

```scala mdoc:silent
import io.getquill.{ idiom => _, _ }
import doobie.quill.DoobieContext
```

We can now construct a `DoobieContext` for our back-end database and import its members, as we would with a traditional Quill context. The options are `H2`, `MySQL`, `Oracle`, `Postgres`, `SQLite`, and `SQLServer`.

```scala mdoc:silent
val dc = new DoobieContext.Postgres(Literal) // Literal naming scheme
import dc._
```

We will be using the `country` table from our test database, so we need a data type of that name, with fields whose names and types line up with the table definition.

```scala mdoc
case class Country(code: String, name: String, population: Int)
```

We're now ready to construct doobie programs using Quill quotes.

**Note:** this is a demonstration of integration and isn't intended to be a Quill tutorial. See the [Quill](https://getquill.io) documentation for information about statement construction.

### Examples

Following are some examples. Note the return types from `run`, which are normal doobie types. You can freely mix Quill quotes into existing doobie programs.

Here is a query.

```scala mdoc
val q1 = quote { query[Country].filter(_.code == "GBR") }

// Select all at once
run(q1)

// Stream in chunks of 16
stream(q1, 16)
```

A simple update.

```scala mdoc
val u1 = quote { query[Country].filter(_.name like "U%").update(_.name -> "foo") }

// Update yielding count of affected rows
run(u1)
```

A batch update.

```scala mdoc
val u2 = quote {
  liftQuery(List("U%", "A%")).foreach { pat =>
    query[Country].filter(_.name like pat).update(_.name -> "foo")
  }
}

// Update yielding list of counts of affected rows
run(u2)
```

Now we will look at batch updates with generated keys. For this we will create a new table.

```sql
CREATE TABLE Foo (
  id    SERIAL,
  value VARCHAR(42)
)
```

And a related data type.

```scala mdoc
case class Foo(id: Int, value: String)
```

We can now write an update returning generated keys.

```scala mdoc
val u3 = quote {
  query[Foo].insert(lift(Foo(0, "Joe"))).returning(_.id)
}

// Update yielding a single id
run(u3)
```

And a batch update returning generated keys.

```scala mdoc
val u4 = quote {
  liftQuery(List(Foo(0, "Joe"), Foo(0, "Bob"))).foreach { a =>
    query[Foo].insert(a).returning(_.id)
  }
}

// Update yielding a list of ids
run(u4)
```



