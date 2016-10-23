name := "spark-nkp"

organization := "com.github.uosdmlab"

version := "0.2.0-SNAPSHOT"

scalaVersion := "2.11.8"

val sparkVersion = "2.0.1"

libraryDependencies ++= Seq(
  "org.bitbucket.eunjeon" %% "seunjeon" % "1.1.1"
    exclude("org.scala-lang", "scala-library") exclude("org.scala-lang", "scala-reflect"),
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
).map(_.exclude("org.slf4j", "slf4j-jdk14")) // For sbt compatibility

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided"
)