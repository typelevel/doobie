
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

lazy val cp = taskKey[Unit]("Copy tut output to blog repo nearby.")

cp := {
  val src = crossTarget.value / "tut"
  val dst = file("../tpolecat.github.io/_book/")
  if (!src.isDirectory) {
    println("Input directory " + dst + " not found.")   
  } else if (!dst.isDirectory) {
    println("Output directory " + dst + " not found.")   
  } else {
    val map = src.listFiles.filter(_.getName.endsWith(".md")).map(f => (f, new File(dst, f.getName)))
    IO.copy(map, true, false)
  }
}

