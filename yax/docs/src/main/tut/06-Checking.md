---
layout: book
number: 6
title: Typechecking Queries
---

<div class="alert alert-warning" role="alert">
<b>Warning:</b> The functionality described in this chapter is experimental and is likely to change in future versions. But it's still pretty freakin' cool.
</div>

In this chapter we learn how to use YOLO mode to validate queries against the database schema and ensure that our type mappings are correct (and if not, get some hints on how to fix them).

### Setting Up

Our setup here is the same as last chapter, so if you're still running from last chapter you can skip this section. Otherwise: imports, `Transactor`, and YOLO mode.

```tut:silent
import doobie.imports._
#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.data._, cats.implicits._
import fs2.interop.cats._
#-cats
val xa = DriverManagerTransactor[IOLite](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
val y = xa.yolo; import y._
```

And again, we're playing with the `country` table, shown here for reference.

```sql
CREATE TABLE country (
  code        character(3)  NOT NULL,
  name        text          NOT NULL,
  population  integer NOT NULL,
  gnp         numeric(10,2),
  indepyear   smallint
  -- more columns, but we won't use them here
)
```

### Checking a Query

In order to create a query that's not quite right, let's redefine our `Country` class with slightly different types.

```tut:silent
case class Country(code: Int, name: String, pop: Int, gnp: Double)
```

Here's our parameterized query from last chapter, but with the new `Country` definition and the `minPop` parameter changed to a `Short`.

```tut:silent
def biggerThan(minPop: Short) = sql"""
  select code, name, population, gnp, indepyear
  from country
  where population > $minPop
""".query[Country]
```

Now let's try the `check` method provided by YOLO and see what happens.

```tut:plain
biggerThan(0).check.unsafePerformIO
```

Yikes, there are quite a few problems, in several categories. In this case **doobie** found

- a parameter coercion that should always work but is not required to be supported by compliant drivers;
- two column coercions that **are** supported by JDBC but are not recommended and can fail in some cases;
- a column nullability mismatch, where a column that is *provably* nullable is read into a non-`Option` type;
- and an unused column.

Suggested fixes are given in terms of both JDBC and vendor-specific schema types and include known custom types like **doobie**'s enumerated `JdbcType`.  Currently this is based on instantiated `Meta` instances, which is not ideal; hopefully in the next release the tooling will improve to support all instances in scope.

Anyway, if we fix all of these problems and try again, we get a clean bill of health.

```tut:silent
case class Country(code: String, name: String, pop: Int, gnp: Option[BigDecimal])

def biggerThan(minPop: Int) = sql"""
  select code, name, population, gnp
  from country
  where population > $minPop
""".query[Country]
```

```tut:plain
biggerThan(0).check.unsafePerformIO
```

**doobie** supports `check` for queries and updates in three ways: programmatically, via YOLO mode in the REPL, and via the `contrib-specs2` package, which allows checking to become part of your unit test suite. We will investigate this in the chapter on testing.

### Working Around Bad Metadata

Some drivers do not implement the JDBC metadata specification very well, which limits the usefulness of the query checking feature. MySQL and MS-SQL do a particularly rotten job in this department. In some cases queries simply cannot be checked because no metadata is available for the prepared statement (manifested as an exception) or the returned metadata is obviously inaccurate.

However a common case is that *parameter* metadata is unavailable but *output column* metadata is. And in these cases there is a workaround: use `checkOutput` rather than `check`. This instructs **doobie** to punt on the input parameters and only check output columns. Unsatisfying but better than nothing.

```tut:plain
biggerThan(0).checkOutput.unsafePerformIO
```

This option is also available in the `contrib-specs2` package.

### Diving Deeper

The `check` logic requires both a database connection and concrete `Meta` instances that define column-level JDBC mappings. This could in principle happen at compile-time, but it's not clear that this is what you always want and it's potentially hairy to implement. So for now checking happens at unit-test time.

The way this works is that a `Query` value has enough type information to describe all parameter and column mappings, as well as the SQL literal itself (with interpolated parameters erased into `?`). From here it is straightforward to prepare the statement, pull the `ResultsetMetaData` and `DatabaseMetaData` and work out whether things are aligned correctly (and if not, determine how misalignments might be fixed). The `Anaylsis` class consumes this metadata and is able to provide the following diagnostics:

- SQL validity. The query must compile, which means it must be consistent with the schema.
- Parameter and column arity. All query inputs and outputs must map 1:1 with parameters and columns.
- Nullability. A parameter or column that is *provably* nullable must be mapped to a Scala `Option`. Note that this is a weak guarantee; columns introduced by an outer join might be nullable but JDBC will tend to report them as "might not be nullable" which isn't useful information.
- Coercibility of types. Mapping of Scala types to JDBC types and JDBC types to vendor types, is asymmetric with respect to reading and writing, and the specification is quite terrible. **doobie** encodes the JDBC spec and combines this with vendor-specific metadata to determine whether a given asserted mapping is sensible or not, and if not, will suggest a fix via changing the Scala type, and another via changing the schema type.
