name := "doobie-contrib-h2"

description := "H2 support for doobie."

libraryDependencies ++= Seq(
  "com.h2database" %  "h2"      % "1.3.170",
  "org.specs2"     %% "specs2"  % "2.4"      % "test"
)

/// PUBLISH SETTINGS

bintrayPublishSettings
