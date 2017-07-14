#+scalaz
package doobie.example

import doobie.imports._
import scalaz.concurrent.{ Task, TaskApp }
import scalaz.stream.Process
import scalaz.stream.Process. { eval, eval_, bracket }
import scalaz.syntax.monad._
import java.sql.{ PreparedStatement, ResultSet }
import doobie.util.process.repeatEvalChunks

/**
 * From a user question on Gitter, how can we have an equivalent to `process[A]` that constructs a
 * stream of untyped maps.
 */
object GenericStream extends TaskApp {

  type Row = Map[String, Any]

  // This escapes to raw JDBC for efficiency.
  def getNextChunkGeneric(chunkSize: Int): ResultSetIO[Seq[Row]] =
    FRS.raw { rs =>
      val md = rs.getMetaData
      val ks = (1 to md.getColumnCount).map(md.getColumnLabel).toList
      var n = chunkSize
      val b = Vector.newBuilder[Row]
      while (n > 0 && rs.next) {
        val mb = Map.newBuilder[String, Any]
        ks.foreach(k => mb += (k -> rs.getObject(k)))
        b += mb.result()
        n -= 1
      }
      b.result()
    }


  def liftProcessGeneric(
    chunkSize: Int,
    create: ConnectionIO[PreparedStatement],
    prep:   PreparedStatementIO[Unit],
    exec:   PreparedStatementIO[ResultSet]): Process[ConnectionIO, Row] = {

    def prepared(ps: PreparedStatement): Process[ConnectionIO, PreparedStatement] =
      eval[ConnectionIO, PreparedStatement] {
        val fs = FPS.setFetchSize(chunkSize)
        FC.lift(ps, fs >> prep).map(_ => ps)
      }

    def unrolled(rs: ResultSet): Process[ConnectionIO, Row] =
      repeatEvalChunks(FC.lift(rs, getNextChunkGeneric(chunkSize)))

    val preparedStatement: Process[ConnectionIO, PreparedStatement] =
      bracket(create)(ps => eval_(FC.lift(ps, FPS.close)))(prepared)

    def results(ps: PreparedStatement): Process[ConnectionIO, Row] =
      bracket(FC.lift(ps, exec))(rs => eval_(FC.lift(rs, FRS.close)))(unrolled)

    preparedStatement.flatMap(results)

  }

  def processGeneric(sql: String, prep: PreparedStatementIO[Unit], chunkSize: Int): Process[ConnectionIO, Row] =
    liftProcessGeneric(chunkSize, FC.prepareStatement(sql), prep, FPS.executeQuery)


  val xa = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

  override def runl(args: List[String]): Task[Unit] =
    args match {
      case sql :: Nil => processGeneric(sql, ().point[PreparedStatementIO], 100).transact(xa).sink(m => Task.delay(Console.println(m)))
      case _          => Task.delay(Console.println("expected on arg, a query"))
    }

  // > runMain doobie.example.GenericStream "select * from city limit 10"
  // Map(name -> Kabul, population -> 1780000, id -> 1, district -> Kabol, countrycode -> AFG)
  // Map(name -> Qandahar, population -> 237500, id -> 2, district -> Qandahar, countrycode -> AFG)
  // Map(name -> Herat, population -> 186800, id -> 3, district -> Herat, countrycode -> AFG)
  // Map(name -> Mazar-e-Sharif, population -> 127800, id -> 4, district -> Balkh, countrycode -> AFG)
  // Map(name -> Amsterdam, population -> 731200, id -> 5, district -> Noord-Holland, countrycode -> NLD)
  // Map(name -> Rotterdam, population -> 593321, id -> 6, district -> Zuid-Holland, countrycode -> NLD)
  // Map(name -> Haag, population -> 440900, id -> 7, district -> Zuid-Holland, countrycode -> NLD)
  // Map(name -> Utrecht, population -> 234323, id -> 8, district -> Utrecht, countrycode -> NLD)
  // Map(name -> Eindhoven, population -> 201843, id -> 9, district -> Noord-Brabant, countrycode -> NLD)
  // Map(name -> Tilburg, population -> 193238, id -> 10, district -> Noord-Brabant, countrycode -> NLD)

}

#-scalaz
