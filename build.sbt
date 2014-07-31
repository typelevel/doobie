
/// SHARED SETTINGS

organization in ThisBuild := "org.tpolecat"

version in ThisBuild := "0.1"

licenses in ThisBuild ++= Seq(("MIT", url("http://opensource.org/licenses/MIT")))

scalaVersion in ThisBuild := "2.10.4"

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

/// SUBMODULES

lazy val core = project.in(file("core"))

lazy val example = project.in(file("example")).dependsOn(core)

