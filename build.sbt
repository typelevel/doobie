name := "doobie"

organization := "org.tpolecat"

version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.10.4"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalacOptions in ThisBuild ++= Seq(
  "-deprecation",           
  "-encoding", "UTF-8", // 2 args
  "-feature",                
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-unchecked",
  "-Xfatal-warnings",       
  "-Xlint",
  "-Yno-adapted-args",       
  "-Ywarn-dead-code",       
  // "-Ywarn-numeric-widen",   
  "-Ywarn-value-discard"     
)

lazy val core = project.in(file("core"))

lazy val test = project.in(file("test")).dependsOn(core)

lazy val example = project.in(file("example")).dependsOn(core)

lazy val free = project.in(file("free"))

