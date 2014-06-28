import FreeGen._

freeGenSettings

freeGenDir := (sourceManaged in Compile).value

freeGenClasses := {
  import java.sql._
  List[Class[_]](
    // classOf[java.sql.Array],
    // classOf[java.sql.NClob],
    // classOf[java.sql.Blob],
    // classOf[java.sql.Clob],
    classOf[java.sql.DatabaseMetaData],
    classOf[java.sql.ResultSetMetaData],
    // classOf[java.sql.Ref],
    // classOf[java.sql.SQLData],
    // classOf[java.sql.SQLInput],
    // classOf[java.sql.SQLOutput],
    classOf[java.sql.Connection],
    classOf[java.sql.Statement],
    classOf[java.sql.PreparedStatement],
    classOf[java.sql.CallableStatement],
    classOf[java.sql.ResultSet]
  )
}

name := "doobie-free"

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

scalacOptions := Seq(
  "-Yno-predef",
  // "-deprecation",           
  "-encoding", "UTF-8", // 2 args
  "-feature",                
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-unchecked",
  // "-Xfatal-warnings",       
  "-Xlint",
  "-Yno-adapted-args",       
  // "-Ywarn-dead-code",       
  // "-Ywarn-numeric-widen",   
  "-Ywarn-value-discard"     
)

scalacOptions in (Compile, doc) ++=
  Seq("-groups")

