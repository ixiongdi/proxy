seq(Revolver.settings: _*)

name := "hello"

version := "1.0"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.9",
  "io.netty" % "netty-codec-socks" % "4.0.21.Final",
  "com.typesafe.akka" %% "akka-remote" % "2.3.4",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4")
