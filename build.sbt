import UnidocKeys._
import FreeGen._
import ReleaseTransformations._

lazy val buildSettings = Seq(
  organization := "org.tpolecat",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", scalaVersion.value)
)

lazy val commonSettings = Seq(
    scalacOptions ++= Seq(
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
    ),
    scalacOptions in (Compile, doc) ++= Seq(
      "-groups",
      "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url", "https://github.com/tpolecat/doobie/tree/v" + version.value + "€{FILE_PATH}.scala",
      "-skip-packages", "scalaz"
    ),
    libraryDependencies ++= macroParadise(scalaVersion.value) ++ Seq(
      "org.scalacheck" %% "scalacheck"  % "1.11.5" % "test",
      "org.specs2"     %% "specs2-core" % "3.6"    % "test"
    ),
    resolvers += "bintray/non" at "http://dl.bintray.com/non/maven",
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.5.2")
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  pomIncludeRepository := Function.const(false),
  pomExtra := (
    <scm>
      <url>git@github.com:tpolecat/doobie.git</url>
      <connection>scm:git:git@github.com:tpolecat/doobie.git</connection>
    </scm>
    <developers>
      <developer>
        <id>tpolecat</id>
        <name>Rob Norris</name>
        <url>http://tpolecat.org</url>
      </developer>
    </developers>
  ),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    ReleaseStep(action = Command.process("package", _)),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _)),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges)
)

lazy val doobieSettings = buildSettings ++ commonSettings

lazy val doobie = project.in(file("."))
  .settings(doobieSettings)
  .settings(noPublishSettings)
  .settings(unidocSettings)
  .settings(unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(example, bench, docs))
  .dependsOn(core, example, postgres, h2, hikari, specs2, docs, bench)
  .aggregate(core, example, postgres, h2, hikari, specs2, docs, bench)

lazy val core = project.in(file("core"))
  .settings(name := "doobie-core")
  .settings(description := "Pure functional JDBC layer for Scala.")
  .settings(doobieSettings ++ publishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang"    %  "scala-reflect"    % scalaVersion.value, // required for shapeless macros
      "org.scalaz"        %% "scalaz-core"      % "7.1.1",
      "org.scalaz"        %% "scalaz-effect"    % "7.1.1",
      "org.scalaz.stream" %% "scalaz-stream"    % "0.7.2a",
      "com.chuusai"       %% "shapeless"        % "2.2.2",
      "com.h2database"    %  "h2"               % "1.3.170" % "test"
    )
  )
  .settings(scalacOptions += "-Yno-predef")
  .settings(freeGenSettings)
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
  .settings(doobieSettings)
  .settings(libraryDependencies ++= Seq(
      "com.h2database" %  "h2"         % "1.3.170",
      "org.scalacheck" %% "scalacheck" % "1.11.5" % "test"
    )
  )
  .settings(scalacOptions += "-deprecation")
  .settings(noPublishSettings)
  .dependsOn(core, postgres, specs2, hikari, h2)

lazy val postgres = project.in(file("contrib/postgresql"))
  .settings(name := "doobie-contrib-postgresql")
  .settings(description := "PostgreSQL support for doobie.")
  .settings(doobieSettings ++ publishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.postgresql" %  "postgresql"   % "9.4-1201-jdbc41",
      "org.postgis"    %  "postgis-jdbc" % "1.3.3" exclude("org.postgis", "postgis-stubs")
    )
  )
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
  .dependsOn(core)

lazy val h2 = project.in(file("contrib/h2"))
  .settings(name := "doobie-contrib-h2")
  .settings(description := "H2 support for doobie.")
  .settings(doobieSettings ++ publishSettings)
  .settings(libraryDependencies += "com.h2database" % "h2"  % "1.3.170")
  .dependsOn(core)

lazy val hikari = project.in(file("contrib/hikari"))
  .settings(name := "doobie-contrib-hikari")
  .settings(description := "Hikari support for doobie.")
  .settings(doobieSettings ++ publishSettings)
  .settings(libraryDependencies += "com.zaxxer" % "HikariCP-java6" % "2.2.5")
  .dependsOn(core)

lazy val specs2 = project.in(file("contrib/specs2"))
  .settings(name := "doobie-contrib-specs2")
  .settings(description := "Specs2 support for doobie.")
  .settings(doobieSettings ++ publishSettings)
  .settings(libraryDependencies += "org.specs2" %% "specs2-core" % "3.6")
  .dependsOn(core)

lazy val docs = project.in(file("doc"))
  .settings(doobieSettings)
  .settings(noPublishSettings)
  .settings(tutSettings)
  .settings(
    initialCommands := """
      import doobie.imports._, scalaz._, Scalaz._, scalaz.concurrent.Task
      val xa = DriverManagerTransactor[Task](
        "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
      )
      """,
    ctut := {
      val src = crossTarget.value / "tut"
      val dst = file("../tpolecat.github.io/_doobie-" + version.value + "/")
      if (!src.isDirectory) {
        println("Input directory " + src + " not found.")
      } else if (!dst.isDirectory) {
        println("Output directory " + dst + " not found.")
      } else {
        println("Copying to " + dst.getPath)
        val map = src.listFiles.filter(_.getName.endsWith(".md")).map(f => (f, new File(dst, f.getName)))
        IO.copy(map, overwrite = true, preserveLastModified = false)
      }
    }
  )
  .settings(libraryDependencies += "io.argonaut" %% "argonaut" % "6.1-M4")
  .dependsOn(core, postgres, specs2, hikari, h2)

lazy val bench = project.in(file("bench"))
  .settings(doobieSettings)
  .settings(noPublishSettings)
  .dependsOn(core, postgres)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

def macroParadise(v: String): List[ModuleID] =
  if (v.startsWith("2.10")) List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
  else Nil

lazy val ctut = taskKey[Unit]("Copy tut output to blog repo nearby.")

