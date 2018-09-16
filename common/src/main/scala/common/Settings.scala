package common

import com.typesafe.config.ConfigFactory


object Settings {

  private val config = ConfigFactory.load()

  val actorSystemName: String = config.getString("akka.actor-system")
  val actorBackendName: String = config.getString("akka.backend-actor-cluster-listener-name")
  val actorFrontendName: String = config.getString("akka.frontend-actor-cluster-listener-name")

  val cassandraAddress: Array[String] = scala.util.Properties.envOrElse("CASSANDRA_ADDRESS", "172.17.0.2").split(",")
  val cassandraKeyspace: String = scala.util.Properties.envOrElse("CASSANDRA_KEYSPACE", "file_io")

  val sparkAppName: String = scala.util.Properties.envOrElse("SPARK_APP_NAME", "FileIO")
  val sparkMaster: String = scala.util.Properties.envOrElse("SPARK_MASTER", "local")

  val webserverAddress = scala.util.Properties.envOrElse("WEBSERVER_ADDRESS", "0.0.0.0")
  val webserverPort = scala.util.Properties.envOrElse("WEBSERVER_PORT", "8112").toInt

  val filesystemPath = scala.util.Properties.envOrElse("FILESYSTEM_PATH", "/home")

}
