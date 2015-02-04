name := "doobie-contrib-hikari"

description := "Hikari support for doobie."

libraryDependencies ++= Seq(
  "com.zaxxer" %  "HikariCP-java6" % "2.2.5"
)

/// PUBLISH SETTINGS

bintrayPublishSettings
