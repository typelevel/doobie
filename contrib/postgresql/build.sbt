name := "doobie-contrib-postgresql"

description := "PostgreSQL support for doobie."

libraryDependencies ++= Seq(
  "org.postgresql" %  "postgresql" % "9.3-1102-jdbc41",
  "org.specs2"     %% "specs2"     % "2.4"               % "test"
)


initialCommands := """
  import scalaz._,Scalaz._
  import scalaz.concurrent.Task
  import doobie.syntax.string._
  import doobie.util.transactor._
  import doobie.contrib.postgresql.pgtypes._
  val xa: Transactor[Task] = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "rnorris", "")
  import xa.yolo._
  """

