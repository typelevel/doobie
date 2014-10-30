name := "doobie-contrib-postgresql"

description := "PostgreSQL support for doobie."

libraryDependencies ++= Seq(
  "org.postgresql" %  "postgresql" % "9.3-1102-jdbc41",
  "org.specs2"     %% "specs2"     % "2.4"               % "test"
)

