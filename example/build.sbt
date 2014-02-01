
name := "doobie-example"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies += "org.scalaz.stream" %% "scalaz-stream" % "0.3"

libraryDependencies ++= Seq(
  "com.h2database"    %  "h2"            % "1.3.170"
)

connectInput in run := true