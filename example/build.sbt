
libraryDependencies ++= Seq(
  "com.h2database" %  "h2"                 % "1.3.170",
  "io.argonaut"    %% "argonaut"           % "6.1-M4",
  "org.http4s"     %% "http4s-core"        % "0.3.0",
  "org.http4s"     %% "http4s-dsl"         % "0.3.0",
  "org.http4s"     %% "http4s-blazeserver" % "0.3.0",
  "org.http4s"     %% "http4s-argonaut"    % "0.3.0"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings"
)

