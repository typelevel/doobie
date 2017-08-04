package doobie.example

import scalaz._, Scalaz._
import scalaz.effect.{ SafeApp, IO }
import scalaz.stream.Process

import doobie.imports._

// Example lifted from slick
object FirstExample extends SafeApp {

  // Our data model
  case class Supplier(id: Int, name: String, street: String, city: String, state: String, zip: String)
  case class Coffee(name: String, supId: Int, price: Double, sales: Int, total: Int)

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
      _  <- DAO.create
      ns <- DAO.insertSuppliers(suppliers)
      nc <- DAO.insertCoffees(coffees)
      _  <- putStrLn(s"Inserted $ns suppliers and $nc coffees.")

      // Select and stream the coffees to stdout
      _ <- DAO.allCoffees.sink(c => putStrLn(c.toString))

      // Get the names and supplier names for all coffees costing less than $9.00,
      // again streamed directly to stdout
      _ <- DAO.coffeesLessThan(9.0).sink(p => putStrLn(p.toString))

      // Same thing, but read into a list this time
      l <- DAO.coffeesLessThan(9.0).list
      _ <- putStrLn(l.toString)

      // Read into a vector this time, with some stream processing
      v <- DAO.coffeesLessThan(9.0).take(2).map(p => (p._1 + "*" + p._2)).vector
      _ <- putStrLn(v.toString)

    } yield "All done!"

  // Entry point for SafeApp
  override def runc: IO[Unit] = {
    val db = DriverManagerTransactor[IO]("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
    for {
      a <- examples.transact(db).attempt
      _ <- IO.putStrLn(a.toString)
    } yield ()
  }

  /** DAO module provides ConnectionIO constructors for end users. */
  object DAO {

    def coffeesLessThan(price: Double): Process[ConnectionIO, (String, String)] =
      Queries.coffeesLessThan(price).process

    def insertSuppliers(ss: List[Supplier]): ConnectionIO[Int] =
      Queries.insertSupplier.updateMany(ss) // bulk insert (!)

    def insertCoffees(cs: List[Coffee]): ConnectionIO[Int] =
      Queries.insertCoffee.updateMany(cs)

    def allCoffees: Process[ConnectionIO, Coffee] =
      Queries.allCoffees.process

    def create: ConnectionIO[Unit] = 
      Queries.create.run.void

  }

  /** Queries module contains "raw" Query/Update values. */
  object Queries {

    def coffeesLessThan(price: Double): Query0[(String, String)] =
      sql"""
        SELECT cof_name, sup_name
        FROM coffees JOIN suppliers ON coffees.sup_id = suppliers.sup_id
        WHERE price < $price
      """.query[(String, String)]

    val insertSupplier: Update[Supplier] =
      Update[Supplier]("INSERT INTO suppliers VALUES (?, ?, ?, ?, ?, ?)", None)

    val insertCoffee: Update[Coffee] =
      Update[Coffee]("INSERT INTO coffees VALUES (?, ?, ?, ?, ?)", None)

    def allCoffees[A]: Query0[Coffee] =
      sql"SELECT cof_name, sup_id, price, sales, total FROM coffees".query[Coffee]

    def create: Update0 = 
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

      """.update

  } 

  // Lifted println
  def putStrLn(s: => String): ConnectionIO[Unit] =
    FC.delay(println(s))

}
