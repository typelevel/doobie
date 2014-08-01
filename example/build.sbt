scalaVersion := "2.10.4"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

resolvers += "tpolecat" at "http://dl.bintray.com/tpolecat/maven"

libraryDependencies += "org.tpolecat" %% "doobie-core" % "0.1"

libraryDependencies += "com.h2database" % "h2" % "1.3.170"

scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings"
)

