---
layout: book
number: 7
title: Inserting and Updating
---

### Setting Up

Same as last chapter, so if you're still set up you can skip this section. 

```tut:silent
import doobie.imports._
import scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.h2.Driver",                      // driver class
  "jdbc:h2:mem:ch7;DB_CLOSE_DELAY=-1",  // connect URL
  "sa", ""                              // user and pass
)
sql"RUNSCRIPT FROM 'world.sql' CHARSET 'UTF-8'".update.run.transact(xa).run
import xa.yolo._
```

And again, playing with the `country` table.

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


### Inserting


### Updating


### Retrieving Results






