
libraryDependencies ++= Seq(
  "com.h2database" %  "h2"                 % "1.3.170",
  "io.argonaut"    %% "argonaut"           % "6.1-M4",
  // N.B this http4s version has to be built from topic/upgrades and published locally
  "org.http4s"     %% "http4s-core"        % "0.3.0-SNAPSHOT",
  "org.http4s"     %% "http4s-dsl"         % "0.3.0-SNAPSHOT",
  "org.http4s"     %% "http4s-blazeserver" % "0.3.0-SNAPSHOT",
  "org.http4s"     %% "http4s-argonaut"    % "0.3.0-SNAPSHOT"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings"
)

