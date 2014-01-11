## doobie - mellow database access

**NOTHING TO SEE HERE ... WORK IN PROGRESS. GO AWAY.**

This is a small, pure-functional JDBC layer for Scala.

### Logging Example

In this contrived example the construction of a `Country` object fails because the developer arbitrarily refuses to recognize New Calendonia. The log is a rose tree reflecting the structure of the computation, where each node labels a computation and reports its result/failure value as well as accumulated time. Tooling will be able to interpret this structure to diagnose errors. 

Note that the structure has almost no relationship to the runtime stack trace because actions are executed in a `Free` interpreter; without a structured log like this it would be very hard to diagnose problems. This is a general issue for functional code where the construction and execution of computations are distinct processes.

The actual console dump shows `[er]` nodes in red and `[ok]` in green. Can't figure out how to do that in markdown, sorry.

```
> runMain doobie.hi.Test
[info] Running doobie.hi.Test 
The answer was: java.lang.RuntimeException: Bogus country: NCL

  hi.examples
  +- [ok] getConnection(jdbc:h2:mem:test               | conn1: url=jdbc:h2:mem:test us    377.440 ms
  `- [er] database session                             | Bogus country: NCL               1181.426 ms
     +- [ok] populate database from /Users/            | false                            1048.743 ms
     |  +- [ok] prepareStatement(RUNSCRIPT FRO         | prep1: RUNSCRIPT FROM ? CHARSE      3.029 ms
     |  `- [ok] process preparedstatement              | false                            1029.587 ms
     |     +- [ok] structured set at index 1: wor      | ()                                  1.295 ms
     |     |  `- [ok] setString(1, world.sql)          | ()                                  0.231 ms
     |     +- [ok] execute                             | false                            1025.518 ms
     |     `- [ok] close                               | ()                                  1.102 ms
     +- [ok] getCatalog                                | TEST                                1.234 ms
     +- [er] find countries where more than            | Bogus country: NCL                 85.326 ms
     |  +- [ok] prepareStatement(SELE...               | prep2: SELECT C.CODE...             2.586 ms
     |  `- [er] process preparedstatement              | Bogus country: NCL                 81.709 ms
     |     +- [ok] structured set at index 1: (Fr      | ()                                  8.719 ms
     |     |  +- [ok] setString(1, French)             | ()                                  0.161 ms
     |     |  `- [ok] setDouble(2, 30.0)               | ()                                  0.586 ms
     |     +- [ok] executeQuery                        | rs11: org.h2.result.LocalResul     15.208 ms
     |     +- [er] process resultset                   | Bogus country: NCL                 48.620 ms
     |     |  +- [ok] next                             | true                                0.235 ms
     |     |  +- [ok] structured get at index 1        | Country(BEL,Belgium,10239000)       9.893 ms
     |     |  |  +- [ok] getString(1)                  | BEL                                 0.214 ms
     |     |  |  +- [ok] getString(2)                  | Belgium                             0.163 ms
     |     |  |  `- [ok] getInt(3)                     | 10239000                            0.222 ms
     |     |  +- [ok] next                             | true                                0.168 ms
     |     |  +- [ok] structured get at index 1        | Country(FRA,France,59225700)        1.209 ms
     |     |  |  +- [ok] getString(1)                  | FRA                                 0.158 ms
     |     |  |  +- [ok] getString(2)                  | France                              0.158 ms
     |     |  |  `- [ok] getInt(3)                     | 59225700                            0.142 ms
     |     |  +- [ok] next                             | true                                0.146 ms
     |     |  +- [ok] structured get at index 1        | Country(MCO,Monaco,34000)           1.145 ms
     |     |  |  +- [ok] getString(1)                  | MCO                                 0.135 ms
     |     |  |  +- [ok] getString(2)                  | Monaco                              0.156 ms
     |     |  |  `- [ok] getInt(3)                     | 34000                               0.157 ms
     |     |  +- [ok] next                             | true                                0.152 ms
     |     |  +- [er] structured get at index 1        | Bogus country: NCL                  1.341 ms
     |     |  |  +- [ok] getString(1)                  | NCL                                 0.155 ms
     |     |  |  +- [ok] getString(2)                  | New Caledonia                       0.145 ms
     |     |  |  `- [ok] getInt(3)                     | 214000                              0.158 ms
     |     |  `- [ok] rs.close                         | ()                                  0.245 ms
     |     `- [ok] close                               | ()                                  0.136 ms
     `- [ok] close                                     | ()                                  0.236 ms

[success] Total time: 3 s, completed Jan 11, 2014 10:33:35 AM
> 
```

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

