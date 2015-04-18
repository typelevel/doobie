
def macroParadise(v: String): List[ModuleID] =
  if (v.startsWith("2.11")) Nil
  else List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))

libraryDependencies ++= Seq(
  "com.h2database" %  "h2"                 % "1.3.170"
) ++ macroParadise(scalaVersion.value)

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.11.5" % "test",
  "org.specs2"     %% "specs2"     % "2.4"    % "test"
)

scalacOptions ++= Seq(
  "-deprecation"
)

publishArtifact := false
