// TODO: how to share with main build?
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.5"

// run dependencyUpdates when we `reload plugins` ... would be nice to run when the main build
// starts up but I don't know how to do that.
onLoad in Global := { s => "dependencyUpdates" :: s }
