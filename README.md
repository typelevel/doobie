## doobie - mellow database access

**NOTHING TO SEE HERE ... WORK IN PROGRESS. GO AWAY.**

This is a small, pure-functional JDBC layer for Scala.

### Logging Example

In this contrived example the construction of a `Country` object fails because the developer arbitrarily refuses to recognize New Calendonia. The log is a rose tree reflecting the structure of the computation, where each node labels a computation and reports its result/failure value as well as accumulated time. Tooling will be able to interpret this structure to diagnose errors. 

Note that the structure has almost no relationship to the runtime stack trace because actions are executed in a `Free` interpreter; without a structured log like this it would be very hard to diagnose problems. This is a general issue for functional code where the construction and execution of computations are distinct processes.

The wrapping is messed up in one place, sorry. Need to sanitize the SQL.

![DigitalKiwi made me do this](log.png)

### Principles

 - Functional and typesafe.
 - Monadic API (no crazy DSL).
 - Write your own damn SQL.
 - Must be impossible to leak any managed resource.
 - Logging must be structured, machine-interpretable, and failure-proof.
 - Resultsets are streams.

### Design Notes

Ok I just rewrote everything but the old stuff is still here. So, deal with it.

The low-level `dbc` package provides a pure monadic mirror of the JDBC 4.0 API, with tree-based logging and resource control (i.e., no leaking). Atop this is the fledgling `hi` package which has the beginnings of an API that you might actually want to use. 

Most of the remainder is moribund but remains to be picked over before deletion.

