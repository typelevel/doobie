
publishArtifact := false

tutSettings

initialCommands := """
import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task
val xa = DriverManagerTransactor[Task](
  "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
)
"""

lazy val ctut = taskKey[Unit]("Copy tut output to blog repo nearby.")

ctut := {
  val src = crossTarget.value / "tut"
  val dst = file("../tpolecat.github.io/_doobie-" + version.value + "/")
  if (!src.isDirectory) {
    println("Input directory " + dst + " not found.")   
  } else if (!dst.isDirectory) {
    println("Output directory " + dst + " not found.")   
  } else {
    println("Copying to " + dst.getPath)
    val map = src.listFiles.filter(_.getName.endsWith(".md")).map(f => (f, new File(dst, f.getName)))
    IO.copy(map, true, false)
  }
}

