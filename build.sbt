import UnidocKeys._

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

lazy val example = project.in(file("example")).dependsOn(core, postgres, specs2, hikari, h2)

lazy val postgres = project.in(file("contrib/postgresql")).dependsOn(core)

lazy val h2 = project.in(file("contrib/h2")).dependsOn(core)

lazy val hikari = project.in(file("contrib/hikari")).dependsOn(core)

lazy val specs2 = project.in(file("contrib/specs2")).dependsOn(core)

lazy val docs = project.in(file("doc")).dependsOn(core, postgres, specs2, hikari, h2)

lazy val bench = project.in(file("bench")).dependsOn(core, postgres)

publishArtifact := false


