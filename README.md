## doobie - mellow database access

**NOTHING TO SEE HERE ... WORK IN PROGRESS. GO AWAY.**

This is a small, pure-functional JDBC layer for Scala.

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

