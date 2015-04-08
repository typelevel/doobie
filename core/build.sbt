import FreeGen._

/// PROJECT METADATA

name := "doobie-core"

description := "Pure functional JDBC layer for Scala."

/// DEPENDENCIES

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

def macroParadise(v: String) =
  if (v.startsWith("2.11")) Nil
  else List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))

libraryDependencies ++= Seq(
  "org.scala-lang"    %  "scala-reflect"    % scalaVersion.value, // required for shapeless macros
  "org.scalaz"        %% "scalaz-core"      % "7.1.1",
  "org.scalaz"        %% "scalaz-effect"    % "7.1.1",
  "org.scalaz.stream" %% "scalaz-stream"    % "0.6a",
  "com.chuusai"       %% "shapeless"        % "2.2.0-RC4"
) ++ macroParadise(scalaVersion.value)

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.11.5" % "test",
  "org.specs2"     %% "specs2"     % "2.4"    % "test"
)

scalacOptions ++= Seq(
  "-Yno-predef"
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

/// BUILD INFO

sourceGenerators in Compile += Def.task {
  val outDir = (sourceManaged in Compile).value / "doobie"
  val outFile = new File(outDir, "buildinfo.scala")
  outDir.mkdirs
  val v = version.value
  val t = System.currentTimeMillis
  IO.write(outFile,
    s"""|package doobie
        |
        |/** Auto-generated build information. */
        |object buildinfo {
        |  /** Current version of doobie ($v). */
        |  val version = "$v"
        |  /** Build date (${new java.util.Date(t)}). */
        |  val date    = new java.util.Date(${t}L)
        |}
        |""".stripMargin)
  Seq(outFile)
}.taskValue


