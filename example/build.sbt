
libraryDependencies ++= Seq(
  "com.h2database" %  "h2"                 % "1.3.170"
)

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.11.5" % "test",
  "org.specs2"     %% "specs2"     % "2.4"    % "test"
)

scalacOptions ++= Seq(
  "-deprecation"
)

publishArtifact := false
