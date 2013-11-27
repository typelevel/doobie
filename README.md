## doobie - mellow database access

This is a small, pure-functional JDBC layer for Scala.

NOTHING TO SEE HERE ... WORK IN PROGRESS. GO AWAY.

### Design Notes

We wrap JDBC in a series of nested monadic interpreters, called **worlds** because that's what I feel like calling them. Each can make calls into the next. You can think of them as dreams within dreams if you're weird like that. 

**Level 1** is the database world. Given connection information this world is able to perform an action in the next world. This is a *transaction*.

**Level 2** is the connection world, which is where most client code will live. In this world we can twiddle properties associated with the connection, commit and rollback, checkpoint, and so on. But most importantly we can, given a hunk of sql and some type assertions, prepare a statement and enter level 3.

**Level 3** is the statement world. In this world we set input parameters prior to execution, at which point we enter level 4.

**Level 4** is the resultset world. In this world we can manipulate the resultset, iterate and accumulate results, and return them up through the stack.

**Level 5** is the row world. It is concerned with reading column values.

The worlds are implemented using a ReaderWriterState pattern over a generalized State monad implemented in Free. Type parameters are existential to prevent leakage of state and readers. Exceptions are caught at each turn of the crank in the low-level Free interpreter, allowing us to halt without losing our writer; i.e., our progress up to the point of failure is preserved and returned to the caller for inspection.




