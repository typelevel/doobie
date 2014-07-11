package doobie.example

import scalaz._, Scalaz._, effect._, IO._
import scalaz.stream._
import doobie._, hi._

import connection.prepareStatement
import preparedstatement.executeQuery
import resultset._

// Example lifted from slick
object FirstExample extends SafeApp {
  import FirstAppModel._
  import FirstExampleDAO._

  def examples: DBIO[String] =
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

      _ <- putStrLn(s"Inserted $s coffees").liftIO[DBIO]

      // Select and stream them to stdout
      _ <- allCoffees.sink(c => IO.putStrLn(c.toString))

      // Coffee names and supplier names for all coffees costing less than $9.00
      // streamed directly to stdout
      _ <- coffeesLessThan(9.0).sink(putLn)

      // Read into a list this time
      l <- coffeesLessThan(9.0).toList
      _ <- putStrLn(l.toString).liftIO[DBIO]

      // Read into a vector this time, with some stream processing
      v <- coffeesLessThan(9.0).take(2).map(p => (p._1 + "*" + p._2)).toVector
      _ <- putStrLn(v.toString).liftIO[DBIO]

    } yield "All done!"

  val ta = DriverManagerTransactor[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

  override def runc: IO[Unit] =
    for {
      a <- ta.exec(examples).except(t => IO(t.toString))
      _ <- putStrLn(a.toString)
    } yield ()

}


object FirstAppModel {

  case class Supplier(id: Int, name: String, street: String, city: String, state: String, zip: String)
  case class Coffee(name: String, supId: Int, price: Double, sales: Int, total: Int)

}

// It's an old but reasonable pattern. If you must have SQL in strings, keep it in one place.
object FirstExampleDAO {
  import FirstAppModel._

  def coffeesLessThan[A](price: Double): Process[DBIO, (String, String)] =
    sql"""
      SELECT cof_name, sup_name
      FROM coffees JOIN suppliers ON coffees.sup_id = suppliers.sup_id
      WHERE price < $price
    """.process[(String, String)]

  def insertSupplier(s: Supplier): DBIO[Int] =
    sql"""
      INSERT INTO suppliers 
      VALUES (${s.id}, ${s.name}, ${s.street}, ${s.city}, ${s.state}, ${s.zip})
    """.executeUpdate

  def insertCoffee(c: Coffee): DBIO[Int] =
    sql"""
      INSERT INTO coffees 
      VALUES (${c.name}, ${c.supId}, ${c.price}, ${c.sales}, ${c.total})
    """.executeUpdate

  def allCoffees[A]: Process[DBIO, Coffee] =
    sql"""
      SELECT cof_name, sup_id, price, sales, total 
      FROM coffees
    """.process[Coffee]

  def create: DBIO[Boolean] = 
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
