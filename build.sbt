name := "doobie"

organization := "org.tpolecat"

version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.10.4"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalacOptions in ThisBuild ++= Seq(
  "-feature", 
  "-deprecation", 
  "-Ywarn-all", // doesn't actually turn them all on :-\
  "-Yno-adapted-args",
  "-Ywarn-value-discard", 
  "-Ywarn-numeric-widen",
  // "-Ywarn-dead-code", // confused by ???, sadly
  "-Xlint",
  "-Xfatal-warnings",
  "-unchecked",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:existentials",
  "-language:experimental.macros"
)

lazy val core = project.in(file("core"))

lazy val test = project.in(file("test")).dependsOn(core)

lazy val example = project.in(file("example")).dependsOn(core)

