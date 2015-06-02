import UnidocKeys._
import FreeGen._

/// SHARED SETTINGS

organization in ThisBuild := "org.tpolecat"

version in ThisBuild := "0.2.3-SNAPSHOT"

licenses in ThisBuild ++= Seq(("MIT", url("http://opensource.org/licenses/MIT")))

scalaVersion in ThisBuild := "2.11.6"

crossScalaVersions in ThisBuild := Seq("2.10.5", scalaVersion.value)

scalacOptions in ThisBuild ++= Seq(
  "-encoding", "UTF-8", // 2 args
  "-feature",                
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",       
  "-Ywarn-dead-code",       
  "-Ywarn-value-discard"     
)

scalacOptions in (Compile, doc) ++= Seq(
  "-groups",
  "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath, 
  "-doc-source-url", "https://github.com/tpolecat/doobie/tree/v" + version.value + "€{FILE_PATH}.scala",
  "-skip-packages", "scalaz"
)

// UNIDOC

resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.5.2")

def macroParadise(v: String): List[ModuleID] =
  if (v.startsWith("2.10")) List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
  else Nil

libraryDependencies in ThisBuild ++= macroParadise(scalaVersion.value)

unidocSettings

unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(example)

/// SUBMODULES

lazy val core = project.in(file("core"))
  .settings(name := "doobie-core")
  .settings(description := "Pure functional JDBC layer for Scala.")
  .settings(resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases")
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang"    %  "scala-reflect"    % scalaVersion.value, // required for shapeless macros
      "org.scalaz"        %% "scalaz-core"      % "7.1.1",
      "org.scalaz"        %% "scalaz-effect"    % "7.1.1",
      "org.scalaz.stream" %% "scalaz-stream"    % "0.7a",
      "com.chuusai"       %% "shapeless"        % "2.2.0"
    )
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.11.5" % "test",
      "org.specs2"     %% "specs2"     % "2.4"    % "test"
    )
  )
  .settings(scalacOptions += "-Yno-predef")
  .settings(autoCompilerPlugins := true)
  .settings(addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.5.2"))
  .settings(libraryDependencies ++= macroParadise(scalaVersion.value))
  .settings(bintrayPublishSettings:_*)
  .settings(freeGenSettings:_*)
  .settings(freeGenDir := (scalaSource in Compile).value / "doobie" / "free")
  .settings(
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
  )
  .settings(
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
  )

lazy val example = project.in(file("example"))
  .dependsOn(core, postgres, specs2, hikari, h2)
  .settings(
    libraryDependencies ++= Seq(
      "com.h2database" %  "h2" % "1.3.170"
    )
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.11.5" % "test",
      "org.specs2"     %% "specs2"     % "2.4"    % "test"
    )
  )
  .settings(scalacOptions += "-deprecation")
  .settings(publishArtifact := false)
  .settings(libraryDependencies ++= macroParadise(scalaVersion.value))

lazy val postgres = project.in(file("contrib/postgresql"))
  .dependsOn(core)
  .settings(name := "doobie-contrib-postgresql")
  .settings(description := "PostgreSQL support for doobie.")
  .settings(
    libraryDependencies ++= Seq(
      "org.postgresql" %  "postgresql"   % "9.4-1201-jdbc41",
      "org.postgis"    %  "postgis-jdbc" % "1.3.3"
    )
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.specs2"     %% "specs2"       % "2.4"               % "test"
    )
  )
  .settings(libraryDependencies ++= macroParadise(scalaVersion.value))
  .settings(
    initialCommands := """
      import scalaz._,Scalaz._
      import scalaz.concurrent.Task
      import doobie.imports._
      import doobie.contrib.postgresql.pgtypes._
      val xa: Transactor[Task] = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
      import xa.yolo._
      import org.postgis._
      import org.postgresql.util._
      import org.postgresql.geometric._
      """
  )
  .settings(bintrayPublishSettings:_*)


lazy val h2 = project.in(file("contrib/h2"))
  .dependsOn(core)
  .settings(name := "doobie-contrib-h2")
  .settings(description := "H2 support for doobie.")
  .settings(
    libraryDependencies ++= Seq(
      "com.h2database" %  "h2"      % "1.3.170",
      "org.specs2"     %% "specs2"  % "2.4"      % "test"
    )
  )
  .settings(libraryDependencies ++= macroParadise(scalaVersion.value))
  .settings(bintrayPublishSettings:_*)


lazy val hikari = project.in(file("contrib/hikari"))
  .dependsOn(core)
  .settings(name := "doobie-contrib-hikari")
  .settings(description := "Hikari support for doobie.")
  .settings(
    libraryDependencies ++= Seq(
      "com.zaxxer" %  "HikariCP-java6" % "2.2.5"
    )
  )
  .settings(libraryDependencies ++= macroParadise(scalaVersion.value))
  .settings(bintrayPublishSettings:_*)

lazy val specs2 = project.in(file("contrib/specs2"))
  .dependsOn(core)
  .settings(name := "doobie-contrib-specs2")
  .settings(description := "Specs2 support for doobie.")
  .settings(libraryDependencies += "org.specs2" %% "specs2-core" % "3.6")
  .settings(libraryDependencies ++= macroParadise(scalaVersion.value))
  .settings(bintrayPublishSettings:_*)

lazy val docs = project.in(file("doc")).dependsOn(core, postgres, specs2, hikari, h2)
  .settings(publishArtifact := false)
  .settings(tutSettings:_*)
  .settings(
    initialCommands := """
      import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task
      val xa = DriverManagerTransactor[Task](
        "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
      )
      """
  )
  .settings(libraryDependencies += "io.argonaut" %% "argonaut" % "6.1-M4")
  .settings(libraryDependencies ++= macroParadise(scalaVersion.value))


lazy val ctut = taskKey[Unit]("Copy tut output to blog repo nearby.")

ctut := {
  val src = crossTarget.value / "tut"
  val dst = file("../tpolecat.github.io/_doobie-" + version.value + "/")
  if (!src.isDirectory) {
    println("Input directory " + dst + " not found.")
  } else if (!dst.isDirectory) {
    println("Output directory " + dst + " not found.")
  } else {
    println("Copying to " + dst.getPath)
    val map = src.listFiles.filter(_.getName.endsWith(".md")).map(f => (f, new File(dst, f.getName)))
    IO.copy(map, true, false)
  }
}



lazy val bench = project.in(file("bench")).dependsOn(core, postgres)
  .settings(publishArtifact := false)

publishArtifact := false


