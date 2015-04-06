name := "doobie-contrib-postgresql"

description := "PostgreSQL support for doobie."

libraryDependencies ++= Seq(
  "org.postgresql" %  "postgresql"   % "9.4-1201-jdbc41",
  "org.postgis"    %  "postgis-jdbc" % "1.3.3",
  "org.specs2"     %% "specs2"       % "2.4"               % "test"
)


initialCommands := """
  import scalaz._,Scalaz._
  import scalaz.concurrent.Task
  import doobie.imports._
  import doobie.contrib.postgresql.pgtypes._
  val xa: Transactor[Task] = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
  import xa.yolo._
  import org.postgis._
  import org.postgresql.util._
  import org.postgresql.geometric._
  """

/// PUBLISH SETTINGS

bintrayPublishSettings
