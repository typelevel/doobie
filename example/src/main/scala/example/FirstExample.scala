package doobie.example

import scalaz._, Scalaz._
import scalaz.effect.{ SafeApp, IO }
import scalaz.stream.Process

import doobie.hi._
import doobie.hi.connection._
import doobie.std.io._
import doobie.syntax.process._
import doobie.syntax.string._
import doobie.syntax.catchable._
import doobie.util.transactor.DriverManagerTransactor

// Example lifted from slick
object FirstExample extends SafeApp {
  import FirstAppModel._, FirstExampleDAO._ 

  // Our database
  val db = DriverManagerTransactor[IO]("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

  // Entry point for SafeApp
  override def runc: IO[Unit] = 
    for {
      a <- db.transact(examples).attempt
      _ <- IO.putStrLn(a.toString)
    } yield ()

  // Lifted println
  def putStrLn(s: => String): ConnectionIO[Unit] =
    delay(println(s))

  // Some suppliers
  val suppliers = List(
    Supplier(101, "Acme, Inc.",      "99 Market Street", "Groundsville", "CA", "95199"),
    Supplier( 49, "Superior Coffee", "1 Party Place",    "Mendocino",    "CA", "95460"),
    Supplier(150, "The High Ground", "100 Coffee Lane",  "Meadows",      "CA", "93966")
  )

  // Some coffees
  val coffees = List(
    Coffee("Colombian",         101, 7.99, 0, 0),
    Coffee("French_Roast",       49, 8.99, 0, 0),
    Coffee("Espresso",          150, 9.99, 0, 0),
    Coffee("Colombian_Decaf",   101, 8.99, 0, 0),
    Coffee("French_Roast_Decaf", 49, 9.99, 0, 0)
  )

  // Our example database action
  def examples: ConnectionIO[String] = 
    for {

      // Create and populate
      _  <- create
      ns <- suppliers.traverseU(insertSupplier).map(_.sum)
      nc <- coffees.traverseU(insertCoffee).map(_.sum)
      _  <- putStrLn(s"Inserted $ns suppliers and $nc coffees.")

      // Select and stream the coffees to stdout
      _ <- allCoffees.sink(c => putStrLn(c.toString))

      // Get the names and supplier names for all coffees costing less than $9.00,
      // again streamed directly to stdout
      _ <- coffeesLessThan(9.0).sink(p => putStrLn(p.toString))

      // Same thing, but read into a list this time
      l <- coffeesLessThan(9.0).list
      _ <- putStrLn(l.toString)

      // Read into a vector this time, with some stream processing
      v <- coffeesLessThan(9.0).take(2).map(p => (p._1 + "*" + p._2)).vector
      _ <- putStrLn(v.toString)

    } yield "All done!"

}


// Our data model
object FirstAppModel {
  case class Supplier(id: Int, name: String, street: String, city: String, state: String, zip: String)
  case class Coffee(name: String, supId: Int, price: Double, sales: Int, total: Int)
}

// It's an old but reasonable pattern. If you must have SQL in strings, keep it in one place.
object FirstExampleDAO {
  import FirstAppModel._

  def coffeesLessThan[A](price: Double): Process[ConnectionIO, (String, String)] =
    sql"""
      SELECT cof_name, sup_name
      FROM coffees JOIN suppliers ON coffees.sup_id = suppliers.sup_id
      WHERE price < $price
    """.query[(String, String)].run

  def insertSupplier(s: Supplier): ConnectionIO[Int] =
    sql"""
      INSERT INTO suppliers 
      VALUES (${s.id}, ${s.name}, ${s.street}, ${s.city}, ${s.state}, ${s.zip})
    """.update.run

  def insertCoffee(c: Coffee): ConnectionIO[Int] =
    sql"""
      INSERT INTO coffees 
      VALUES (${c.name}, ${c.supId}, ${c.price}, ${c.sales}, ${c.total})
    """.update.run

  def allCoffees[A]: Process[ConnectionIO, Coffee] =
    sql"""
      SELECT cof_name, sup_id, price, sales, total 
      FROM coffees
    """.query[Coffee].run

  def create: ConnectionIO[Unit] = 
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

    """.update.run.void

} 
