---
layout: book
number: 2
title: Toolkit
---

Before we get into writing **doobie** programs it's worth spending a moment to explain the big picture. The short explanation is:

> **doobie** programs are values. You can compose several small programs to build a larger program. Once you have constructed a program you wish to run, you interpret it into an effectiful target monad of your choice (`Task` or `IO` for example) and drop it into your main application wherever you like.

### Fistful of Monads

**doobie** is a monadic API that provides a number of monads that all work the same way but describe computations in different contexts. For example, `ConnectionIO` describes a computation in a context where a `java.sql.Connection` is available; `ResultSetIO` is similar but requires a `java.sql.ResultSet`. 

