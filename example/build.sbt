
libraryDependencies ++= Seq(
  "com.h2database" %  "h2"                 % "1.3.170",
  "io.argonaut"    %% "argonaut"           % "6.1-M4",
  "org.http4s"     %% "http4s-core"        % "0.3.0",
  "org.http4s"     %% "http4s-dsl"         % "0.3.0",
  "org.http4s"     %% "http4s-blazeserver" % "0.3.0",
  "org.http4s"     %% "http4s-argonaut"    % "0.3.0"
)

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.11.5" % "test",
  "org.specs2"     %% "specs2"     % "2.4"    % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings"
)

initialCommands := """
  import scalaz._,Scalaz._
  import scalaz.concurrent.Task

  import doobie.imports._
  import doobie.contrib.postgresql.pgtypes._
  
  import org.postgresql.util._
  import org.postgresql.geometric._

  // Lame, how can we fix this?
  doobie.contrib.postgresql.pgtypes

  val xa: Transactor[Task] = DriverManagerTransactor[Task](
    "org.postgresql.Driver", 
    "jdbc:postgresql:world", 
    "rnorris", 
    ""
  )
  import xa.yolo._
  """

