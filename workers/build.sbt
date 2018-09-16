import sbtassembly.MergeStrategy

name := "backend-workers"

version := "1.0"

scalaVersion := "2.12.4"

lazy val common = RootProject(file("../common"))
val main = Project(id = "workers", base = file(".")).dependsOn(common)

val akkaVersion = "2.5.6"
val jacksonVersion = "2.8.8"
val akkaKafka = "0.17"
val akkaCassandra = "0.14"
val kamon = "0.6.7"

libraryDependencies ++= Seq(
  // DataStax Java Driver For Apache Cassandra Core
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.3.0",

  // Akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-agent" % akkaVersion,
  "com.typesafe.akka" %% "akka-camel" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-osgi" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-tck" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion,
  "com.typesafe.akka" %% "akka-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion,

  // Akka Streams Kafka
  "com.typesafe.akka" %% "akka-stream-kafka" % akkaKafka,

  // JSON
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,

  // Kamon
  "io.kamon" %% "kamon-core" % kamon,
  "io.kamon" %% "kamon-statsd" % kamon,
  "io.kamon" %% "kamon-datadog" % kamon
)

resolvers += "Akka Snapshots" at "http://repo.akka.io/snapshots/"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case conf :String if conf.contains(".conf") => MergeStrategy.concat
  case _ => MergeStrategy.first
}

mainClass in assembly := Some("Worker")