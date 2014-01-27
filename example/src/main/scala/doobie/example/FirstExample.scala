package doobie.example

import scalaz._, Scalaz._, effect._, IO._
import scalaz.stream._
import doobie._, hi._

import kleisliEffect._
import connection._
import preparedstatement._
import resultset._

// Example lifted from slick
object FirstExample extends SafeApp {
  import FirstAppModel._
  import FirstExampleDAO._

  def examples: Connection[String] =
    for {

      // Create our database
      _ <- create

      // Insert some suppliers
      _ <- insertSupplier(Supplier(101, "Acme, Inc.",      "99 Market Street", "Groundsville", "CA", "95199"))
      _ <- insertSupplier(Supplier( 49, "Superior Coffee", "1 Party Place",    "Mendocino",    "CA", "95460"))
      _ <- insertSupplier(Supplier(150, "The High Ground", "100 Coffee Lane",  "Meadows",      "CA", "93966"))

      // Insert some coffees (no batch update yet)
      s <- List(
            Coffee("Colombian",         101, 7.99, 0, 0),
            Coffee("French_Roast",       49, 8.99, 0, 0),
            Coffee("Espresso",          150, 9.99, 0, 0),
            Coffee("Colombian_Decaf",   101, 8.99, 0, 0),
            Coffee("French_Roast_Decaf", 49, 9.99, 0, 0)
          ).traverseU(insertCoffee).map(_.sum)

      _ <- putStrLn(s"Inserted $s coffees").liftIO[Connection]

      // Select and print them out
      _ <- allCoffees(sink[Coffee](c => putStrLn(c.toString)))

      // Coffee names and supplier names for all coffees costing less than $9.00
      _ <- coffeesLessThan(9.0)(sink[(String, String)](c => putStrLn(c.toString)))

      // Read into a list this time
      l <- coffeesLessThan(9.0)(list[(String, String)])
      _ <- putStrLn(l.toString).liftIO[Connection]

      // Read into a vector this time (typesafe! look, no type params!)
      // Can we get the lifting right and say coffeesLessThan0(9.0).take(1) ... runLog ??
      v <- coffeesLessThan0(9.0)(_.take(1).map(p => (p._1 + "*" + p._2)).runLog)
      _ <- putStrLn(v.toString).liftIO[Connection]

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

}


object FirstAppModel {

  case class Supplier(id: Int, name: String, street: String, city: String, state: String, zip: String)
  case class Coffee(name: String, supId: Int, price: Double, sales: Int, total: Int)

}

// It's an old but reasonable pattern. If you must have SQL in strings, keep it in one place.
object FirstExampleDAO {
  import FirstAppModel._

  def coffeesLessThan[A](price: Double)(k: ResultSet[A]): Connection[A] =
    connection.push(s"coffeesLessThan($price)") {
      sql"""
      SELECT cof_name, sup_name
      FROM coffees JOIN suppliers ON coffees.sup_id = suppliers.sup_id
      WHERE price < $price
      """.executeQuery(k)
    }

  def coffeesLessThan0[A](price: Double)(k: Process[ResultSet, (String, String)] => ResultSet[A]): Connection[A] =
    coffeesLessThan(price)(k(process[(String, String)]))

  // TODO: what i'd really like  to see ^^ is
  // def coffeesLessThan0(price: Double): Processs[Connection, (String, String)] = ...
  // which i think we should be able to

  def insertSupplier(s: Supplier): Connection[Int] =
    connection.push(s"insertSupplier($s))") {
      sql"""
      INSERT INTO suppliers 
      VALUES (${s.id}, ${s.name}, ${s.street}, ${s.city}, ${s.state}, ${s.zip})
      """.executeUpdate
    }

  def insertCoffee(c: Coffee): Connection[Int] =
    connection.push(s"insertCoffee($c))") {
      sql"""
      INSERT INTO coffees 
      VALUES (${c.name}, ${c.supId}, ${c.price}, ${c.sales}, ${c.total})
      """.executeUpdate
    }

  // An issue here is that we haven't asserted the row type anywhere, so the caller just kind of
  // needs to know. The issue is that ResultSet[A] is too general a type.
  def allCoffees[A](k: ResultSet[A]): Connection[A] =
    connection.push(s"allCoffees") {
      sql"""
      SELECT cof_name, sup_id, price, sales, total 
      FROM coffees
      """.executeQuery(k)
    }

  def create: Connection[Boolean] = 
    connection.push("create") {
      sql"""

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

      """.execute
    }

} 
