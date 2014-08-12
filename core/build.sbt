import FreeGen._

/// PROJECT METADATA

name := "doobie-core"

description := "Pure functional JDBC layer for Scala."

/// DEPENDENCIES

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

def shapeless(v: String) = 
  if (v.startsWith("2.11")) "com.chuusai" %%  "shapeless"       % "2.0.0"
  else                      "com.chuusai" %  ("shapeless_" + v) % "2.0.0"

libraryDependencies ++= Seq(
  "org.scala-lang"    %  "scala-reflect"    % scalaVersion.value, // required for shapeless macros
  "org.scalaz"        %% "scalaz-core"      % "7.1.0",
  "org.scalaz"        %% "scalaz-effect"    % "7.1.0",
  "org.scalaz.stream" %% "scalaz-stream"    % "0.5a",
  shapeless(scalaVersion.value)
)

libraryDependencies ++= Seq(     
  "org.scalacheck" %% "scalacheck" % "1.11.5" % "test",
  "org.specs2"     %% "specs2"     % "2.4"    % "test"
)

scalacOptions ++= Seq(
  "-Yno-predef"
)

scalacOptions in (Compile, doc) ++= Seq(
  "-groups",
  "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath, 
  "-doc-source-url", "https://github.com/tpolecat/doobie/tree/master€{FILE_PATH}.scala" // master for now
)

/// PUBLISH SETTINGS

bintrayPublishSettings

/// CODE GENERATION SETTINGS

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


