---
layout: book
number: 2
title: Toolkit
---

Before we get into writing **doobie** programs it's worth spending a moment to explain the big picture. The short explanation is:

> **doobie** programs are values. You can compose small programs to build larger programs. Once you have constructed a program you wish to run, you interpret it into an effectiful target monad of your choice (`Task` or `IO` for example) and drop it into your main application wherever you like.

### Nested Programs

**doobie** is a monadic API that provides a number of monads that all work the same way but describe computations in different contexts. For example `ConnectionIO` describes computations that take place in a context where a `java.sql.Connection` is available. Similar monads exist for each major type in `java.sql`. Programs written in these contexts compose naturally via lifting, mirroring the lifecycles of each contexts's carrier type.

<p class="text-center"><img src="/assets/nesting.png"></p>

Some patterns of composition are so common and generic that they can be provided for you, as **doobie** does with its high-level API. As a result many programs can be written entirely in terms of `ConnectionIO`. Some examples are:

- Performing a query and reading the results as a stream.
- Performing an update and returning updated rows as a stream.
- Validating a query or update in terms of the schema and JDBC ~ Scala type mappings.

This book largely focuses on these common interactions, but also explains their representation at lower levels in case you wish to do something other than what you get for free.

### Low and High

**doobie** provides two APIs intended for different audiences.

The **low-level API** in `doobie.free` provides a direct 1:1 mapping to the underlying JDBC API, giving library implementors a pure functional wrapper for the full JDBC 4.0 API. This API provides no resource safety, `NULL` checking, or type mapping; and it directly exposes lifetime-managed JDBC objects; programs written at this level have desirable compositional properties but still require a great deal of care.

The **high-level API** in `doobie.hi` (implemented entirely in terms of the low-level API) provides a safe subset of the JDBC API. Programs written with this API have no access to the underlying JDBC objects and cannot leak references. Features of this API include:

- Typeclass-based mapping of scalar, array, and vendor-specific types to columns.
- Typeclass-based mapping of product types to rows.
- Trivial custom two-way row/column mapping via invariant functors.
- Nullability via `Option` (Scala `null` cannot be observed).
- Proper enumerated types for all JDBC constants.
- Result sets as scalaz streams.
- Typesafe string interpolator for SQL literals.

As noted above, the types used for both APIs are identical; the difference lies only in the exposed constructors. This means that a program otherwise written in the `doobie.hi` API can use constuctors from `doobie.free` to implement advanced or vendor-specific behavior directly, without translation or lifting.

