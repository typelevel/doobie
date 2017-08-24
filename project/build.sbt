// TODO: how to share with main build?
libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.1.1", // needed by flyway
  "org.slf4j"      % "slf4j-nop"  % "1.7.21"  // to silence some log messages
)
