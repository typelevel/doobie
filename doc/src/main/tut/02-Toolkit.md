## 2. Toolkit

Before we get into **doobie** itself it's worth spending a moment to explain the big picture, and how this stuff fits into the rest of your program. The short answer is:

> **doobie** programs are values that can be composed to build larger programs. Eventually these are interpreted into a target monad of your choice (typically `scalaz.concurrent.Task`).

### Fistful of Monads

**doobie** is a monadic API that provides a number of monads that all work the same way but describe computations in different contexts. For example, `ConnectionIO` describes a computation in a context where a `java.sql.Connection` is available; `ResultSetIO` is similar but requires a `java.sql.ResultSet`. 