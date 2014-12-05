## 6. Checking Queries

### Setting Up

Same as last chapter, so if you're still set up you can skip this section. 

```tut:silent
import doobie.imports._
import scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.h2.Driver",                      // driver class
  "jdbc:h2:mem:ch6;DB_CLOSE_DELAY=-1", // connect URL
  "sa", ""                              // user and pass
)
```

Sample data and YOLO mode.

```tut
sql"RUNSCRIPT FROM 'world.sql' CHARSET 'UTF-8'".update.run.transact(xa).run
import xa.yolo._
```

And our `Country` class.

```tut:silent
case class Country(code: String, name: String, pop: Int, gnp: Option[Double])
```


### Parameterized Queries

Ok here's our parameterized query from last chapter.

```tut
def biggerThan(minPop: Int) = sql"""
  select code, name, population, gnp 
  from country
  where population > $minPop
""".query[Country]
```

It seems to work, but how do we know for sure?

```tut
biggerThan(0).check.run
```

