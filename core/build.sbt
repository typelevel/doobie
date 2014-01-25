name := "doobie-core"

// scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits")

libraryDependencies ++= Seq(
  "org.scalaz"        %% "scalaz-core"   % "7.0.4",
  "org.scalaz"        %% "scalaz-effect" % "7.0.4",
  "io.argonaut"       %% "argonaut"      % "6.0.1"   ,
  "com.chuusai"       %  "shapeless"     % "2.0.0-M1" cross CrossVersion.full
)
