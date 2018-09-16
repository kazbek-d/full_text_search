package common

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import common.Settings.actorSystemName

object AkkaImplicits {

  private val port = scala.util.Properties.envOrElse("WORKER_PORT", "0")
  private val config = ConfigFactory
    .parseString(s"akka.remote.netty.tcp.port=$port").
    withFallback(ConfigFactory.load())

  implicit val system = ActorSystem(actorSystemName, config)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

}