import FreeGen._

freeGenSettings

freeGenDir := (scalaSource in Compile).value / "doobie" / "free"

freeGenClasses := {
  import java.sql._
  List[Class[_]](
    classOf[java.sql.NClob],
    classOf[java.sql.Blob],
    classOf[java.sql.Clob],
    classOf[java.sql.DatabaseMetaData],
    classOf[java.sql.Driver],
    classOf[java.sql.Ref],
    classOf[java.sql.SQLData],
    classOf[java.sql.SQLInput],
    classOf[java.sql.SQLOutput],
    classOf[java.sql.Connection],
    classOf[java.sql.Statement],
    classOf[java.sql.PreparedStatement],
    classOf[java.sql.CallableStatement],
    classOf[java.sql.ResultSet]
  )
}

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "org.scala-lang"    %  "scala-reflect"    % scalaVersion.value, // required for shapeless macros
  "org.scalaz"        %% "scalaz-core"      % "7.1.0-RC2",
  "org.scalaz"        %% "scalaz-effect"    % "7.1.0-RC2",
  "org.scalaz.stream" %% "scalaz-stream"    % "0.4.1a",
  "io.argonaut"       %% "argonaut"         % "6.1-M2",
  "com.chuusai"       %  "shapeless_2.10.4" % "2.0.0"
)

libraryDependencies ++= Seq(     
  "org.scalacheck" %% "scalacheck" % "1.10.1" % "test",
  "org.specs2"     %% "specs2"     % "1.12.3" % "test"
)

scalacOptions ++= Seq(
  "-Yno-predef"
)

scalacOptions in (Compile, doc) ++=
  Seq("-groups")
