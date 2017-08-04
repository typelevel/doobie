import FreeGen2._
import ReleaseTransformations._

// Library versions all in one place, for convenience and sanity.
lazy val scalaCheckVersion    = "1.13.5"
lazy val specs2Version        = "3.9.4"
lazy val si2712fixVersion     = "1.2.0"
lazy val kindProjectorVersion = "0.9.3"
lazy val shapelessVersion     = "2.3.2"
lazy val sourcecodeVersion    = "0.1.3"
lazy val h2Version            = "1.4.195"
lazy val postgresVersion      = "42.1.1"
lazy val fs2CoreVersion       = "0.9.6"
lazy val fs2CatsVersion       = "0.3.0"
lazy val postGisVersion       = "2.2.1"
lazy val hikariVersion        = "2.6.1"
lazy val scalatestVersion     = "3.0.3"
lazy val refinedVersion       = "0.8.1"
lazy val argonautVersion      = "6.2"
lazy val paradiseVersion      = "2.1.0"
lazy val circeVersion         = "0.8.0"
lazy val monixVersion         = "2.3.0"
lazy val catsVersion          = "0.9.0"

val postgisDep = "net.postgis" % "postgis-jdbc" % postGisVersion

lazy val buildSettings = Seq(
  organization := "org.tpolecat",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  scalaVersion := "2.12.3",
  crossScalaVersions := Seq("2.11.11", scalaVersion.value)
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
      "-Ywarn-value-discard",
      "-Ypartial-unification"
    ),
    scalacOptions in (Compile, doc) ++= Seq(
      "-groups",
      "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url", "https://github.com/tpolecat/doobie/tree/v" + version.value + "€{FILE_PATH}.scala"
    ),
    scalacOptions in (Compile, console) --= Seq(
      "-Xlint"
    ),
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck"        % scalaCheckVersion % "test",
      "org.specs2"     %% "specs2-core"       % specs2Version     % "test",
      "org.specs2"     %% "specs2-scalacheck" % specs2Version     % "test"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % kindProjectorVersion)
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
    pushChanges),
  mappings in (Compile, packageSrc) ++= (managedSources in Compile).value pair relativeTo(sourceManaged.value / "main" / "scala")
)

lazy val doobieSettings = buildSettings ++ commonSettings

lazy val doobie = project.in(file("."))
  .settings(doobieSettings)
  .settings(noPublishSettings)
  .dependsOn(core, h2, hikari, postgres, specs2, example, bench, scalatest, docs, refined)
  .aggregate(core, h2, hikari, postgres, specs2, example, bench, scalatest, docs, refined)
  .settings(freeGen2Settings)
  .settings(
    freeGen2Dir := file("modules/core/src/main/scala/doobie/free"),
    freeGen2Classes := {
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

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val core = project
  .in(file("modules/core"))
  .settings(doobieSettings)
  .settings(publishSettings)
  .settings(
    name := "doobie-core",
    description := "Pure functional JDBC layer for Scala.",
    libraryDependencies ++= Seq(
      scalaOrganization.value %  "scala-reflect" % scalaVersion.value, // required for shapeless macros
      "com.chuusai"           %% "shapeless"     % shapelessVersion,
      "com.lihaoyi"           %% "sourcecode"    % sourcecodeVersion
    ),
    scalacOptions += "-Yno-predef",
    sourceGenerators in Compile += Def.task {
      val outDir = (sourceManaged in Compile).value / "scala" / "doobie"
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
    }.taskValue,
    libraryDependencies ++= Seq(
      "co.fs2"         %% "fs2-core"  % fs2CoreVersion,
      "co.fs2"         %% "fs2-cats"  % fs2CatsVersion,
      "org.typelevel"  %% "cats-core" % catsVersion,
      "org.typelevel"  %% "cats-free" % catsVersion,
      "org.typelevel"  %% "cats-laws" % catsVersion % "test",
      "com.h2database" %  "h2"        % h2Version   % "test"
    )
  )

lazy val example = project
  .in(file("modules/example"))
  .settings(doobieSettings ++ noPublishSettings)
  .dependsOn(core, postgres, specs2, scalatest, hikari, h2)

lazy val postgres = project
  .in(file("modules/postgres"))
  .dependsOn(core)
  .settings(doobieSettings)
  .settings(publishSettings)
  .settings(
    name  := "doobie-postgres",
    description := "Postgres support for doobie.",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % postgresVersion,
      postgisDep % "provided"
    ),
    initialCommands := """
      import doobie.imports._
      import doobie.postgres.imports._
      val xa = DriverManagerTransactor[IOLite]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
      val yolo = xa.yolo
      import yolo._
      import org.postgis._
      import org.postgresql.util._
      import org.postgresql.geometric._
      """
  )

lazy val h2 = project
  .in(file("modules/h2"))
  .settings(doobieSettings)
  .settings(publishSettings)
  .dependsOn(core)
  .settings(
    name  := "doobie-h2",
    description := "H2 support for doobie.",
    libraryDependencies += "com.h2database" % "h2"  % h2Version
  )

lazy val hikari = project
  .in(file("modules/hikari"))
  .dependsOn(core)
  .settings(doobieSettings)
  .settings(publishSettings)
  .settings(
    name := "doobie-hikari",
    description := "Hikari support for doobie.",
    libraryDependencies += "com.zaxxer" % "HikariCP" % hikariVersion
  )

lazy val specs2 = project
  .in(file("modules/specs2"))
  .dependsOn(core)
  .dependsOn(h2 % "test")
  .settings(doobieSettings)
  .settings(publishSettings)
  .settings(
    name := "doobie-specs2",
    description := "Specs2 support for doobie.",
    libraryDependencies += "org.specs2" %% "specs2-core" % specs2Version
  )

lazy val scalatest = project
  .in(file("modules/scalatest"))
  .dependsOn(core)
  .settings(doobieSettings)
  .settings(publishSettings)
  .settings(
    name := s"doobie-scalatest",
    description := "Scalatest support for doobie.",
    libraryDependencies ++= Seq(
      "org.scalatest"  %% "scalatest" % scalatestVersion,
      "com.h2database"  %  "h2"       % h2Version % "test"
    )
  )

lazy val bench = project
  .in(file("modules/bench"))
  .dependsOn(core, postgres)
  .settings(doobieSettings)
  .settings(noPublishSettings)

lazy val docs = project
  .in(file("modules/docs"))
  .dependsOn(core, postgres, specs2, hikari, h2, scalatest)
  .settings(doobieSettings)
  .settings(noPublishSettings)
  .settings(tutSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe"    %% "circe-core"    % circeVersion,
      "io.circe"    %% "circe-generic" % circeVersion,
      "io.circe"    %% "circe-parser"  % circeVersion,
      "io.argonaut" %% "argonaut"      % argonautVersion,
      "io.monix"    %% "monix-eval"    % monixVersion
    ),
    fork in Test := true,
    // postgis is `provided` dependency for users, and section from book of doobie needs it
    libraryDependencies += postgisDep
  )

lazy val refined = project
  .in(file("modules/refined"))
  .dependsOn(core)
  .settings(doobieSettings)
  .settings(publishSettings)
  .settings(
    name := "doobie-refined",
    description := "Refined support for doobie.",
    libraryDependencies ++= Seq(
      "eu.timepit"            %% "refined"        % refinedVersion,
      scalaOrganization.value %  "scala-compiler" % scalaVersion.value % Provided,
      "com.h2database"        %  "h2"             % h2Version          % "test"
    )
  )
