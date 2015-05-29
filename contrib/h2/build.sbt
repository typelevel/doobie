name := "doobie-contrib-h2"

description := "H2 support for doobie."

libraryDependencies ++= Seq(
  "com.h2database" %  "h2"      % "1.3.170",
  "org.specs2"     %% "specs2"  % "2.4"      % "test"
)

def macroParadise(v: String): List[ModuleID] =
  if (v.startsWith("2.10")) List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
  else Nil

libraryDependencies ++= macroParadise(scalaVersion.value)

/// PUBLISH SETTINGS

bintrayPublishSettings
