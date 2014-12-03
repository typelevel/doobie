
libraryDependencies ++= Seq(
  "com.h2database" %  "h2" % "1.3.170"
)

publishArtifact := false

tutSettings

initialCommands := """
import doobie.imports._
import scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.h2.Driver",                      // driver class
  "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", // connect URL
  "sa", ""                              // user and pass
)
"""