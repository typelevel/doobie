name := "doobie-contrib-h2"

description := "H2 support for doobie."

def macroParadise(v: String) =
  if (v.startsWith("2.11")) Nil
  else List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))

libraryDependencies ++= Seq(
  "com.h2database" %  "h2"      % "1.3.170",
  "org.specs2"     %% "specs2"  % "2.4"      % "test"
) ++ macroParadise(scalaVersion.value)

/// PUBLISH SETTINGS

bintrayPublishSettings
