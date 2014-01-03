## dbc

The `dbc` package provides a functional, resource-safe interface to the complete JDBC 4.0 API, with the goal that any JDBC program should be expressable directly. As such this is quite low-level; most users will not use this API directly.

We provide a tower of effect worlds, each equivalent to `ReaderT[IO,S,A]` but providing no direct access to the state and no public `run` method. Allocated resources such as `Statement` objects are not returned directly, but are instead passed to a continuation in an effect world with the appopriate state type and closed when the continuation exits. So instead of calling methods on a `ResultSet` object you perform a computation in a `ResultSet[A]` context. Ultimately each computation interacting with the database runs in `Connection[A]` which is executable via a `Database` object.

It is very important to note that the operations defined here map 1:1 with JDBC calls and **do not** massage the values produced and consumed in most cases; this is left to higher-level APIs. Some actions consume or produce mutable values like arrays and streams, and impure use of these values *must* be lifted into `IO` and then into the proper effect world via `liftIO` in order to keep your computations pure. Likewise, many actions consume or produce **null values**; lifting into `Option` is again left a task for higher-level APIs in order to keep `dbc` actions consistent with their JDBC equivalents.

