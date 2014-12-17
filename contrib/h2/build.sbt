name := "doobie-contrib-h2"

description := "H2 support for doobie."

libraryDependencies ++= Seq(
  "com.h2database" %  "h2"      % "1.3.170",
  "org.specs2"     %% "specs2"  % "2.4"      % "test"
)


initialCommands := """
  import scalaz._,Scalaz._
  import scalaz.concurrent.Task
  import doobie.syntax.string._
  import doobie.util.transactor._
  import doobie.contrib.h2.h2types._
  val xa: Transactor[Task] = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
  import xa.yolo._
  """

