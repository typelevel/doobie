name := "doobie-core"

// scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits")

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "org.scala-lang"    %  "scala-reflect"    % scalaVersion.value, // required for shapeless macros
  "org.scalaz"        %% "scalaz-core"      % "7.1.0-M7",
  "org.scalaz"        %% "scalaz-effect"    % "7.1.0-M7",
  "org.scalaz.stream" %% "scalaz-stream"    % "0.4.1a",
  "io.argonaut"       %% "argonaut"         % "6.1-M2",
  "com.chuusai"       %  "shapeless_2.10.4" % "2.0.0"
)

