import UnidocKeys._
import FreeGen._
import ReleaseTransformations._
import OsgiKeys._

lazy val buildSettings = Seq(
  organization := "org.tpolecat",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", scalaVersion.value, "2.12.0-M3")
)

lazy val commonSettings = Seq(
    scalacOptions ++= Seq(
      "-encoding", "UTF-8", // 2 args
      "-feature",
      "-deprecation",
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
      "org.scalacheck" %% "scalacheck"  % "1.13.0" % "test",
      "org.specs2"     %% "specs2-core" % "3.7.1"  % "test"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1")
)

lazy val publishSettings = osgiSettings ++ Seq(
  exportPackage := Seq("doobie.*"),
  privatePackage := Seq(),
  dynamicImportPackage := Seq("*"),
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  homepage := Some(url("https://github.com/tpolecat/doobie")),
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
  // .settings(unidocSettings)
  // .settings(unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(example, bench, docs))
  .dependsOn(core, core_cats, h2, h2_cats, hikari, hikari_cats, postgres, postgres_cats, specs2, specs2_cats, example, example_cats, bench, bench_cats) //, docs, docs_cats
  .aggregate(core, core_cats, h2, h2_cats, hikari, hikari_cats, postgres, postgres_cats, specs2, specs2_cats, example, example_cats, bench, bench_cats) //, docs, docs_cats
  .settings(freeGenSettings)
  .settings(
    freeGenDir := file("yax/core/src/main/scala/doobie/free"),
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

// Workaround to avoid cyclic dependency
// TODO remove after tut-core and argonaut for Scala 2.12 is released
lazy val tuut = taskKey[Seq[(File, String)]]("Temporary task to conditionally skip tut")

// Temporarily skip tut for Scala 2.12
// TODO remove after tut-core and argonaut for Scala 2.12 is released
lazy val docSkipScala212Settings = Seq(
  libraryDependencies ++= {
    if (scalaVersion.value startsWith "2.12") Nil
    else Seq("io.argonaut" %% "argonaut" % "6.2-M1")
  },
  tuut := Def.taskDyn {
    if (scalaVersion.value startsWith "2.12")
      Def.task(Seq.empty[(File, String)])
    else
      Def.task(tut.value)
  }.value
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

def macroParadise(v: String): List[ModuleID] =
  if (v.startsWith("2.10")) List(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
  else Nil

lazy val ctut = taskKey[Unit]("Copy tut output to blog repo nearby.")

///
/// CORE
///

def coreSettings(mod: String) = 
  doobieSettings  ++ 
  publishSettings ++ Seq(
    name := "doobie-" + mod,
    description := "Pure functional JDBC layer for Scala.",
    libraryDependencies ++= Seq(
      "org.scala-lang" %  "scala-reflect" % scalaVersion.value, // required for shapeless macros
      "com.chuusai"    %% "shapeless"     % "2.3.0"
    ),
    scalacOptions += "-Yno-predef",
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

lazy val core = project.in(file("modules/core"))
  .enablePlugins(SbtOsgi)
  .settings(
    yax(file("yax/core"), "scalaz"),
    coreSettings("core"),
    libraryDependencies ++= Seq(
      "org.scalaz"        %% "scalaz-core"   % "7.2.0",
      "org.scalaz"        %% "scalaz-effect" % "7.2.0",
      "org.scalaz.stream" %% "scalaz-stream" % "0.8a",
      "com.h2database"    %  "h2"            % "1.3.170" % "test"
    )
  )

lazy val core_cats = project.in(file("modules-cats/core"))
  .enablePlugins(SbtOsgi)
  .settings(
    yax(file("yax/core"), "cats", "fs2"),
    coreSettings("core-cats"),
    libraryDependencies ++= Seq(
      "org.typelevel"  %% "cats"     % "0.6.0",
      "co.fs2"         %% "fs2-core" % "0.9.0-M5",
      // "co.fs2"         %% "fs2-cats" % "0.1.0-M6",
      "com.h2database" %  "h2"       % "1.3.170" % "test"
    )
  )

///
/// EXAMPLE
///

lazy val example = project.in(file("modules/example"))
  .settings(doobieSettings ++ noPublishSettings)
  .settings(yax(file("yax/example"), "scalaz"))
  .dependsOn(core, postgres, specs2, hikari, h2)

lazy val example_cats = project.in(file("modules-cats/example"))
  .settings(doobieSettings ++ noPublishSettings)
  .settings(yax(file("yax/example"), "cats"))
  .dependsOn(core_cats, postgres_cats, specs2_cats, hikari_cats, h2_cats)

///
/// POSTGRES
///

def postgresSettings(mod: String): Seq[Setting[_]] =
  doobieSettings  ++
  publishSettings ++ Seq(
    name  := "doobie-" + mod,
    description := "Postgres support for doobie.",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql"   % "9.4-1201-jdbc41",
      "org.postgis"    % "postgis-jdbc" % "1.3.3" exclude("org.postgis", "postgis-stubs")
    ),
    initialCommands := """
      import doobie.imports._
      import doobie.postgres.pgtypes._
      val xa = DriverManagerTransactor[IOLite]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
      import xa.yolo._
      import org.postgis._
      import org.postgresql.util._
      import org.postgresql.geometric._
      """
  )

lazy val postgres = project.in(file("modules/postgres"))
  .enablePlugins(SbtOsgi)
  .settings(
    yax(file("yax/postgres"), "scalaz"),
    postgresSettings("postgres")
  )
  .dependsOn(core)

lazy val postgres_cats = project.in(file("modules-cats/postgres"))
  .enablePlugins(SbtOsgi)
  .settings(
    yax(file("yax/postgres"), "cats"),
    postgresSettings("postgres-cats")
  )
  .dependsOn(core_cats)

///
/// H2
///

def h2Settings(mod: String): Seq[Setting[_]] =
  doobieSettings  ++
  publishSettings ++ Seq(
    name  := "doobie-" + mod,
    description := "H2 support for doobie.",
    libraryDependencies += "com.h2database" % "h2"  % "1.3.170"
  )

lazy val h2 = project.in(file("modules/h2"))
  .enablePlugins(SbtOsgi)
  .settings(
    yax(file("yax/h2"), "scalaz"),
    h2Settings("h2")
  )
  .dependsOn(core)

lazy val h2_cats = project.in(file("modules-cats/h2"))
  .enablePlugins(SbtOsgi)
  .settings(
    yax(file("yax/h2"), "cats"),
    h2Settings("h2-cats")
  )
  .dependsOn(core_cats)

///
/// HIKARI
///

def hikariSettings(mod: String): Seq[Setting[_]] =
  doobieSettings  ++ 
  publishSettings ++ Seq(
    name := "doobie-" + mod,
    description := "Hikari support for doobie.",
    libraryDependencies += "com.zaxxer" % "HikariCP-java6" % "2.2.5"
  )

lazy val hikari = project.in(file("modules/hikari"))
  .enablePlugins(SbtOsgi)
  .settings(
    yax(file("yax/hikari"), "scalaz"),
    hikariSettings("hikari")
  )
  .dependsOn(core)

lazy val hikari_cats = project.in(file("modules-cats/hikari"))
  .enablePlugins(SbtOsgi)
  .settings(
    yax(file("yax/hikari"), "cats"),
    hikariSettings("hikari-cats")
  )
  .dependsOn(core_cats)

///
/// SPECS2
///

def specs2Settings(mod: String): Seq[Setting[_]] =
  doobieSettings  ++ 
  publishSettings ++ Seq(
    name := "doobie-contrib-specs2",
    description := "Specs2 support for doobie.",
    libraryDependencies += "org.specs2" %% "specs2-core" % "3.7.1"
  )

lazy val specs2 = project.in(file("modules/specs2"))
  .enablePlugins(SbtOsgi)
  .settings(
    yax(file("yax/specs2"), "scalaz"),
    specs2Settings("specs2")
  )
  .dependsOn(core)

lazy val specs2_cats = project.in(file("modules-cats/specs2"))
  .enablePlugins(SbtOsgi)
  .settings(
    yax(file("yax/specs2"), "cats"),
    specs2Settings("specs2")
  )
  .dependsOn(core_cats)

///
/// BENCH
///

lazy val bench = project.in(file("modules/bench"))
  .settings(doobieSettings ++ noPublishSettings)
  .settings(yax(file("yax/bench"), "scalaz"))
  .dependsOn(core, postgres)

lazy val bench_cats = project.in(file("modules-cats/bench"))
  .settings(doobieSettings ++ noPublishSettings)
  .settings(yax(file("yax/bench"), "cats"))
  .dependsOn(core_cats, postgres_cats)

///
/// DOCS
///

def docsSettings(tokens: String*): Seq[Setting[_]] =
  doobieSettings          ++
  noPublishSettings       ++
  tutSettings             ++ 
  docSkipScala212Settings ++ Seq(
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
    },
    tutSourceDirectory := sourceManaged.value / "main" / "tut",
    tutPluginJars := {
      // piggyback on a task tut depends on, so yax runs first
      yax.walk(file("yax/docs/src/main/tut"), sourceManaged.value / "main", tokens.toSet)
      tutPluginJars.value
    }
  )

lazy val docs = project.in(file("modules/docs"))
  .settings(docsSettings("scalaz"))
  .dependsOn(core, postgres, specs2, hikari, h2)


lazy val docs_cats = project.in(file("modules-cats/docs"))
  .settings(docsSettings("cats", "fs2"))
  .dependsOn(core_cats, postgres_cats, specs2_cats, hikari_cats, h2_cats)
