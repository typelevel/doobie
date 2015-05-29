name := "doobie-contrib-postgresql"

description := "PostgreSQL support for doobie."

libraryDependencies ++= Seq(
  "org.postgresql" %  "postgresql"   % "9.4-1201-jdbc41",
  "org.postgis"    %  "postgis-jdbc" % "1.3.3",
  "org.specs2"     %% "specs2"       % "2.4"               % "test"
)

def macroParadise(v: String): List[ModuleID] =
  if (v.startsWith("2.10")) List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
  else Nil

libraryDependencies ++= macroParadise(scalaVersion.value)

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
