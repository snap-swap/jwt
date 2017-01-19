name := "jwt"

organization := "com.snapswap"

version := "1.0.0"

scalaVersion := "2.11.8"

scalacOptions := Seq(
  "-feature",
  "-unchecked",
  "-deprecation",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Ywarn-unused-import",
  "-encoding",
  "UTF-8")

libraryDependencies ++= Seq(
  "io.spray" %% "spray-json" % "1.3.2",
  "commons-codec" % "commons-codec" % "1.10",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)