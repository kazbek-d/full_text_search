package actors.sink

import akka.Done
import akka.actor.{Actor, ActorLogging}
import model.RestApi.SinkTaskKafkaCassandra


class CassandraSink(sinkTask: SinkTaskKafkaCassandra) extends Actor with ActorLogging {

  override def receive: Receive = {
    case Done =>
      println("Done comes")
  }

}
