name := "spark-nkp"

version := "0.1.0"

scalaVersion := "2.11.8"

val sparkVersion = "2.0.1"

libraryDependencies ++= Seq(
  "org.bitbucket.eunjeon" %% "seunjeon" % "1.1.1",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided"
)