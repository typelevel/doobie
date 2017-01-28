#+cats
// relies on whenM, etc. so no cats for now
#-cats
#+scalaz
package doobie.example

import doobie.imports._
import doobie.postgres.imports._

import java.io.File

import scalaz._, Scalaz._, scalaz.concurrent.Task
import scalaz.stream._
import scodec.bits.ByteVector
import org.postgresql.copy.{CopyIn, CopyOut}

object postgrescopymanager {

  case class City(
    id: Int,
    name: String,
    countryCode: String,
    district: String,
    population: Int
  )

  val cities =
    City(1400, "Mazar-e-Sharif", "AFG", "Balkh", 127800) ::
    City(1500, "Amsterdam", "NLD", "Noord-Holland", 731200) ::
    City(1600, "Rotterdam", "NLD", "Zuid-Holland", 593321) ::
    City(1700, "Haag", "NLD", "Zuid-Holland", 440900) ::
    City(1800, "Utrecht", "NLD", "Utrecht", 234323) ::
    City(1900, "Eindhoven", "NLD", "Noord-Brabant", 201843) :: Nil

  def cityToFields(city: City): String =
    // string escaping is omitted for simplicity
    List(
      city.id.shows,
      city.name,
      city.countryCode,
      city.district,
      city.population.shows
    ).intercalate("|")


  lazy val xa = Transactor.fromDriverManager[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

  object inputstream {

    val citiesProcess: Process[Task, City] =
      Process.emitAll(cities)

    def stringToByteVector(str: String): ByteVector =
      ByteVector.view(str.getBytes("UTF-8"))

    def prog(cities: Process[Task, City]): CopyManagerIO[Long] =
      PFCM.copyIn(
        "COPY city FROM STDIN DELIMITER '|'",
        io.toInputStream(
          cities
            .map(cityToFields)
            .intersperse("\n")
            .map(stringToByteVector)
        )
      )

    val task: Task[Unit] =
      PHC.pgGetCopyAPI(prog(citiesProcess)).transact(xa) >>= { count =>
        Task.delay(Console.println(s"$count records inserted"))
      }

    def main(args: Array[String]): Unit =
      task.unsafePerformSync
  }


  object copyinprocess {

    val copyInSink: Sink[CopyInIO, ByteVector] =
      Process.repeatEval {
        PFCI.delay((bytes: ByteVector) => {
          val barr = bytes.toArray
          PFCI.writeToCopy(barr, 0, barr.length)
        })
      }.onComplete(Process.eval_(PFCI.endCopy.void))

    val copyOutProcess: Process[CopyOutIO, ByteVector] =
      Process.repeatEval[CopyOutIO, ByteVector] {
        PFCO.readFromCopy >>= { bytes =>
          PFCO.delay {
            Option(bytes).map(ByteVector.view)
              .getOrElse(throw Cause.Terminated(Cause.End))
          }
        }
      }

    val citiesProcess: Process[CopyInIO, City] =
      Process.emitAll(cities)


    def stringToByteVector(str: String): ByteVector =
      ByteVector.view(str.getBytes("UTF-8"))

    val stdOutLines: Sink[CopyOutIO, String] =
      Process.constant((s: String) => PFCO.delay(println(s)))

    val outProcess: Process[CopyOutIO, Unit] =
      copyOutProcess
        .map(b => new String(b.toArray, "UTF-8"))
        .flatMap(s => Process.emitAll(s.split("\n")))
        .map { s =>
          // naive parsing. error handling is omitted for simplicity
          val fields = s.split(",")
          City(fields(0).toInt, fields(1), fields(2), fields(3), fields(4).toInt)
        }
        .map(_.toString)
        .to(stdOutLines)

    val progOut: CopyManagerIO[Unit] =
      PFCM.copyOut("copy city to stdout (encoding 'utf-8', format csv)") >>= {
        outProcess.run.transK[CopyManagerIO].run(_)
      }

    val progIn: CopyManagerIO[Unit] =
      PFCM.copyIn("COPY city FROM STDIN DELIMITER '|'") >>= {
        citiesProcess
          .map(cityToFields)
          .intersperse("\n")
          .map(stringToByteVector)
          .to(copyInSink)
          .run.transK[CopyManagerIO].run(_)
      }

    val task: Task[Unit] =
      PHC.pgGetCopyAPI(progIn *> progOut).transact(xa)

    def main(args: Array[String]): Unit =
      task.unsafePerformSync
  }


  object recaptured {

    def copyInSink(ci: => CopyIn): Sink[Task, ByteVector] =
      io.resource(Task.delay(ci))(ci => Task.delay(ci.endCopy()).void) { ci =>
        Task.now((bytes: ByteVector) => Task.delay {
          val barr = bytes.toArray
          ci.writeToCopy(barr, 0, barr.length)
        })
      }

    def copyOutProcess(co: => CopyOut): Process[Task, ByteVector] =
      io.resource(Task.delay(co))(_ => ().point[Task]) { co =>
        Task.delay {
          val bytes = co.readFromCopy()
          Option(bytes).map(ByteVector.view)
            .getOrElse(throw Cause.Terminated(Cause.End))
        }
      }

    val citiesProcess: Process[Task, City] =
      Process.emitAll(cities)

    def stringToByteVector(str: String): ByteVector =
      ByteVector.view(str.getBytes("UTF-8"))

    val progIn: CopyManagerIO[Unit] =
      PFCM.copyIn("COPY city FROM STDIN DELIMITER '|'") >>= { copyIn =>
        Capture[CopyManagerIO].apply {
          citiesProcess
            .map(cityToFields)
            .intersperse("\n")
            .map(stringToByteVector)
            .to(copyInSink(copyIn))
            .run.unsafePerformSync // side-effect is re-captured in CopyManagerIO
        }
      }

    val progOut: CopyManagerIO[Unit] =
      PFCM.copyOut("copy city to stdout (encoding 'utf-8', format csv)") >>= { copyOut =>
        Capture[CopyManagerIO].apply {
          copyOutProcess(copyOut)
            .map(b => new String(b.toArray, "UTF-8"))
            .flatMap(s => Process.emitAll(s.split("\n")))
            .map { s =>
              // naive parsing. error handling is omitted for simplicity
              val fields = s.split(",")
              City(fields(0).toInt, fields(1), fields(2), fields(3), fields(4).toInt)
            }
            .map(_.toString)
            .to(io.stdOutLines)
            .run.unsafePerformSync // side-effect is re-captured in CopyManagerIO
        }
      }

    val task: Task[Unit] =
      PHC.pgGetCopyAPI(progIn *> progOut).transact(xa)

    def main(args: Array[String]): Unit =
      task.unsafePerformSync
  }

}
#-scalaz
