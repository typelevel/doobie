## dbc

The `dbc` package provides a functional, resource-safe interface for JDBC. This API is quite low-level (users will normally use higher-level combinators built atop `dbc`) but it does provide access to essentially all of JDBC's functionalty. 

This package provides a tower of effect worlds, each equivalent to `ReaderT[IO,S,A]` but providing no  access to the state and no public `run` method. Allocated resources such as `Statement` objects are not returned directly, but passed to a continuation in an effect world with the appopriate state type and closed when the continuation exits. The entry point is the `Database` world, which can execute `Connection` actions given a `ConnectInfo`.

The operations defined here **do not** massage the values produced and consumed; this is left to higher-level APIs. Some actions consume or produce mutable values like arrays and streams, and impure use of these values *must* be lifted into `IO` and then into the proper effect world via `liftIO` in order to keep your computation pure. Likewise, many actions consume or produce **null values**; lifting into `Option` is again left a task for higher-level APIs in order to keep `dbc` actions consistent with their JDBC equivalents.




