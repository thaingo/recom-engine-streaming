import sbt.Keys.libraryDependencies

name := "recom-engine-streaming"

version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.4.1" % "provided",
  "org.apache.spark" %% "spark-sql" % "2.4.1" % "provided",
  "org.apache.spark" %% "spark-mllib" % "2.4.1" % "provided",
  "org.apache.spark" %% "spark-streaming" % "2.4.1" % "provided",
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.4.1" excludeAll
    ExclusionRule("org.spark-project.spark", "unused"),
  "org.apache.kafka" % "kafka-clients" % "2.3.0" % "provided",
  "org.mongodb.spark" %% "mongo-spark-connector" % "2.4.1"
)