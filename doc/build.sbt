
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

libraryDependencies += "io.argonaut" %% "argonaut" % "6.1-M4"

def macroParadise(v: String): List[ModuleID] =
  if (v.startsWith("2.11")) Nil
  else List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))

libraryDependencies ++= macroParadise(scalaVersion.value)

