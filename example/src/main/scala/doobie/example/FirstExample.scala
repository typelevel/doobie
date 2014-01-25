package doobie.example

import scalaz._, Scalaz._, effect._, IO._
import doobie._, dbc._, hi._

import connection._
import preparedstatement._

// Example lifted from slick
object FirstExample extends SafeApp {

  case class Supplier(id: Int, name: String, street: String, city: String, state: String, zip: String)
  case class Coffee(name: String, supId: Int, price: Double, sales: Int, total: Int)

  def examples: Connection[String] =
    for {

      // Create ouru database
      _ <- DAO.create

      // Insert some suppliers
      _ <- DAO.insertSupplier(Supplier(101, "Acme, Inc.",      "99 Market Street", "Groundsville", "CA", "95199"))
      _ <- DAO.insertSupplier(Supplier( 49, "Superior Coffee", "1 Party Place",    "Mendocino",    "CA", "95460"))
      _ <- DAO.insertSupplier(Supplier(150, "The High Ground", "100 Coffee Lane",  "Meadows",      "CA", "93966"))

      // Insert some coffees (no batch update yet)
      _ <- List(
            Coffee("Colombian",         101, 7.99, 0, 0),
            Coffee("French_Roast",       49, 8.99, 0, 0),
            Coffee("Espresso",          150, 9.99, 0, 0),
            Coffee("Colombian_Decaf",   101, 8.99, 0, 0),
            Coffee("French_Roast_Decaf", 49, 9.99, 0, 0)
          ).traverseU(DAO.insertCoffee)

      // Select and print them out
      _ <- DAO.allCoffees(sink[Coffee](c => putStrLn(c.toString)))

  //   // Perform a join to retrieve coffee names and supplier names for
  //   // all coffees costing less than $9.00
  //   println("Manual join:")
  //   val q2 = for {
  //     c <- coffees if c.price < 9.0
  //     s <- suppliers if s.id === c.supID
  //   } yield (c.name, s.name)
  //   for(t <- q2) println("  " + t._1 + " supplied by " + t._2)

  //   // Do the same thing using the navigable foreign key
  //   println("Join by foreign key:")
  //   val q3 = for {
  //     c <- coffees if c.price < 9.0
  //     s <- c.supplier
  //   } yield (c.name, s.name)
  //   // This time we read the result set into a List
  //   val l3: List[(String, String)] = q3.list
  //   for((s1, s2) <- l3) println("  " + s1 + " supplied by " + s2)

  //   // Check the SELECT statement for that query
  //   println(q3.selectStatement)

  //   // Compute the number of coffees by each supplier
  //   println("Coffees per supplier:")
  //   val q4 = (for {
  //     c <- coffees
  //     s <- c.supplier
  //   } yield (c, s)).groupBy(_._2.id).map {
  //     case (_, q) => (q.map(_._2.name).min.get, q.length)
  //   }
  //   // .get is needed because Slick cannot enforce statically that
  //   // the supplier is always available (being a non-nullable foreign key),
  //   // thus wrapping it in an Option
  //   q4 foreach { case (name, count) =>
  //     println("  " + name + ": " + count)
  //   }
  // }

    } yield "All done!"

  val database: IO[Database] =
    Database[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

  override def runc: IO[Unit] =
    for {
      d <- database 
      l <- util.TreeLogger.newLogger(LogElement("FirstExample"))
      a <- d.run(examples, l).except(t => IO(t.toString))
      _ <- putStrLn(a.toString)
      _ <- l.dump
    } yield ()


  // It's an old but reasonable pattern. If you must have SQL in strings, keep it in one place.
  object DAO {

    def insertSupplier(s: Supplier): Connection[Int] =
      connection.push(s"insertSupplier($s))",
      sql"INSERT INTO suppliers VALUES (${s.id}, ${s.name}, ${s.street}, ${s.city}, ${s.state}, ${s.zip})".executeUpdate)

    def insertCoffee(c: Coffee): Connection[Int] =
      connection.push(s"insertCoffee($c))",
      sql"INSERT INTO coffees VALUES (${c.name}, ${c.supId}, ${c.price}, ${c.sales}, ${c.total})".executeUpdate)

    def allCoffees[A](rs: ResultSet[A]): Connection[A] =
      connection.push(s"allCoffees",
      sql"SELECT cof_name, sup_id, price, sales, total FROM coffees".executeQuery(rs))

    def create: Connection[Boolean] = 
      connection.push("create", sql"""

        CREATE TABLE suppliers (
          sup_id   INT     NOT NULL PRIMARY KEY,
          sup_name VARCHAR NOT NULL,
          street   VARCHAR NOT NULL,
          city     VARCHAR NOT NULL,
          state    VARCHAR NOT NULL,
          zip      VARCHAR NOT NULL        
        );

        CREATE TABLE coffees (
          cof_name VARCHAR NOT NULL,
          sup_id   INT     NOT NULL,
          price    DOUBLE  NOT NULL,
          sales    INT     NOT NULL,
          total    INT     NOT NULL
        );

        ALTER TABLE coffees
        ADD CONSTRAINT coffees_suppliers_fk FOREIGN KEY (sup_id) REFERENCES suppliers(sup_id);

        """.execute)

    }

}