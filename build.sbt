import sbt.Keys._
import sbtassembly.Plugin.AssemblyKeys._

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  Resolver.url("artifactory", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns),
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "maxaf-releases" at s"http://repo.bumnetworks.com/releases/"
)

lazy val versionRegex = "(\\d+)\\.(\\d+).*".r
lazy val sparkVersion: SettingKey[String] = settingKey[String]("Spark version")
lazy val currentSparkVersion=util.Properties.propOrElse("sparkVersion", "1.6.2")
lazy val mistScalaCrossCompile = currentSparkVersion match {
  case versionRegex("1", minor) => Seq("2.10.6")
  case _ => Seq("2.11.8")
}

lazy val commonSettings = Seq(
  organization := "io.hydrosphere",

  sparkVersion := currentSparkVersion,
  scalaVersion := (
    sparkVersion.value match {
      case versionRegex("1", minor) => "2.10.6"
      case _ => "2.11.8"
    }),

  crossScalaVersions := mistScalaCrossCompile,
  version := "0.11.0"
)

lazy val spark2AdditionalDependencies = currentSparkVersion match {
  case versionRegex("1", minor) => Seq()
  case _ => Seq(
    "org.json4s" %% "json4s-native" % "3.2.10",
    "org.apache.parquet" % "parquet-column" % "1.7.0",
    "org.apache.parquet" % "parquet-hadoop" % "1.7.0",
    "org.apache.parquet" % "parquet-avro" % "1.7.0",

    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.slf4j" % "slf4j-api" % "1.7.5" % "test",
    "org.slf4j" % "slf4j-log4j12" % "1.7.5" % "test"
  )
}

def sparkDependencies(v: String) =
  Seq(
    "org.apache.spark" %% "spark-core" % v % "provided",
    "org.apache.spark" %% "spark-sql" % v % "provided",
    "org.apache.spark" %% "spark-hive" % v % "provided",
    "org.apache.spark" %% "spark-streaming" % v % "provided",
    "org.apache.spark" %% "spark-mllib" % v % "provided"
  )

lazy val mistLib = project.in(file("."))
  .settings(assemblySettings)
  .settings(commonSettings: _*)
  .settings(
    name := "mist-lib",
    libraryDependencies ++= sparkDependencies(currentSparkVersion),
    libraryDependencies ++= spark2AdditionalDependencies,
    libraryDependencies ++= Seq(
      "org.apache.kafka" % "kafka-clients" % "0.8.2.0" exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-log4j12"),
      "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.1.0"
    )
  )

lazy val commonAssemblySettings = Seq(
  mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
    case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
    case m if m.startsWith("META-INF") => MergeStrategy.discard
    case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
    case PathList("org", "apache", xs@_*) => MergeStrategy.first
    case PathList("org", "jboss", xs@_*) => MergeStrategy.first
    case "about.html" => MergeStrategy.rename
    case "reference.conf" => MergeStrategy.concat
    case PathList("org", "datanucleus", xs@_*) => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
  },
  test in assembly := {}
)
