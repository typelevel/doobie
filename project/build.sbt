// TODO: how to share with main build?
libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.1.4", // needed by flyway
  "org.slf4j"      % "slf4j-nop"  % "1.7.25"  // to silence some log messages
)

// run dependencyUpdates when we `reload plugins` ... would be nice to run when the main build
// starts up but I don't know how to do that.
onLoad in Global := { s => "dependencyUpdates" :: s }
