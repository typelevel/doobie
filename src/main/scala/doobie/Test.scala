package doobie

import java.sql._
import doobie.std._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz._
import Scalaz._
import java.io.File
import scalaz.stream._

// TODO: clean this up; ignore imports for now
import doobie.world._
import doobie.world.connection.{ Action => DBIO, unit => dbio, _ }
import doobie.world.database.ConnectInfo
import doobie.world.resultset.stream

object Test extends SafeApp with ExperimentalSytax {

  // This import needs to be here. Why? Fix it
  import doobie.std.default._ 

  // An in-memory database
  val ci = ConnectInfo[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

  // Some model types (note nesting)
  case class Id[A](toInt: Int)
  case class CountryCode(code: String)
  case class City(id: Id[City], name: String, code: CountryCode, population: Int)

  // Primitive mapping for files (just an example)
  implicit val pfile: Primitive[File] = 
    implicitly[Primitive[String]].xmap[File](new File(_), _.getAbsolutePath) as "java.io.File"

  // DDL statement using a file as a parameter
  def loadDatabase(f: File): DBIO[Unit] =
   q"""
      RUNSCRIPT FROM $f
      CHARSET 'UTF-8'
    """.asUnit

  // The `count` largest cities.
  def largestCities(count: Int): DBIO[Vector[City]] =
   q"""
      SELECT id, name, countrycode, name --population 
      FROM city 
      ORDER BY population DESC
    """.stream[City].pipe(process1.take(count)).foldMap(Vector(_))

  // Find countries that speak a given language
  def speakerQuery(s: String, p: Int): DBIO[Vector[CountryCode]] =
   q"""
      SELECT COUNTRYCODE 
      FROM COUNTRYLANGUAGE 
      WHERE LANGUAGE = $s AND PERCENTAGE > $p
    """.stream[CountryCode].foldMap(Vector(_))

  // We can compose many DBIO computations
  def action: DBIO[String] =
    for {
      _ <- dbio(println("Loading database..."))
      _ <- loadDatabase(new File("world.sql"))
      s <- speakerQuery("French", 30)
      _ <- s.traverse(s => dbio(println(s)))
      c <- largestCities(10)
      _ <- c.traverse(s => dbio(println(s)))
    } yield "woo!"

  // Apply connection info to a DBIO[A] to get an IO[(Log, Throwable \/ A)]
  override def runc: IO[Unit] =
    for {
      p <- action.run.run(ci)
      _ <- putStrLn("")
      _ <- explain.database.showLog(p._1)
      _ <- putStrLn("")
      _ <- putStrLn(p._2.toString) // the answer
    } yield ()

}

//////////


trait ExperimentalSytax {

  // Let's try interpolation syntax. This only works for primitives
  implicit class SqlInterpolator(val sc: StringContext) {

    def sql: String = 
      sc.parts.mkString("?")

    def source[I: Composite](i:I): SourceMaker =
      SourceMaker(sql, statement.setC(i))

    def q(): SourceMaker =
      SourceMaker(sql, statement.unit(()))

    def q[A: Primitive](a: A): SourceMaker =
      source(a)

    def q[A: Primitive, B: Primitive](a: A, b: B): SourceMaker =
      source((a,b))

  }

  case class SourceMaker(sql0: String, prep0: statement.Action[Unit]) {

    def stream[O:Composite]: Source[O] =
      new Source[O] {
        val sql = sql0
        val prep = prep0
        type I = O
        val I = Composite[I]
        val p1 = process1.id[I]
      }

    def asUnit: DBIO[Unit] =
      (prep0 >> statement.execute).run(sql0)

  }

}



trait Source[O] { outer =>

  type I
  implicit def I:Composite[I]
  def sql: String
  def prep: statement.Action[Unit]
  def p1: Process1[I,O]

  def pipe[B](p: Process1[O,B]): Source[B] =
    new Source[B] {
      val sql = outer.sql
      val prep = outer.prep
      type I = outer.I
      val I = outer.I
      val p1 = outer.p1 pipe p
    }

  // terminating combinator; this lifts us to DBIO
  def foldMap[B: Monoid](f: O => B): DBIO[B] =
    (prep >> stream[I].pipe(p1).foldMap(f).run).run(sql)

}


