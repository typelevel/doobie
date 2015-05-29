name := "doobie-contrib-hikari"

description := "Hikari support for doobie."

libraryDependencies ++= Seq(
  "com.zaxxer" %  "HikariCP-java6" % "2.2.5"
)

def macroParadise(v: String): List[ModuleID] =
  if (v.startsWith("2.10")) List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
  else Nil

libraryDependencies ++= macroParadise(scalaVersion.value)

/// PUBLISH SETTINGS

bintrayPublishSettings
