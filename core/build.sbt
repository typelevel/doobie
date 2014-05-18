name := "doobie-core"

// scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits")

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies += "org.scalaz.stream" %% "scalaz-stream" % "0.3"

libraryDependencies ++= Seq(
  "org.scala-lang"    %  "scala-reflect"    % scalaVersion.value, // required for shapeless macros
  "org.scalaz"        %% "scalaz-core"      % "7.0.5",
  "org.scalaz"        %% "scalaz-effect"    % "7.0.5",
  "io.argonaut"       %% "argonaut"         % "6.0.1",
  "com.chuusai"       %  "shapeless_2.10.4" % "2.0.0"
)

