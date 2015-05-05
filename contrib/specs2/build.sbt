name := "doobie-contrib-specs2"

description := "Specs2 support for doobie."

libraryDependencies += "org.specs2" %% "specs2" % "2.4"

def macroParadise(v: String): List[ModuleID] =
  if (v.startsWith("2.10")) List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
  else Nil

libraryDependencies ++= macroParadise(scalaVersion.value)

/// PUBLISH SETTINGS

bintrayPublishSettings
