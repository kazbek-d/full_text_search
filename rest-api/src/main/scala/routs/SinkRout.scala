package routs

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import model.RestApi._
import model._

import scala.concurrent.duration._

class SinkRout(actorRef: ActorRef)  extends BaseRout {

  val timeout10Minutes = 10.minute

  import common.RestApiImplicits._

  implicit val timeout: Timeout = timeout10Minutes

  def getPath(sinkableType: SinkableType) = post {
    withRequestTimeout(timeout10Minutes) {
      entity(as[SinkParams]) { request =>
        onComplete(actorRef ? Sink(Start, Some(SinkTaskKafkaCassandra(sinkableType, request))))(makeResult)
      }
    }
  } ~
    delete {
      withRequestTimeout(timeout10Minutes) {
        entity(as[SinkParams]) { request =>
          onComplete(actorRef ? Sink(Stop, Some(SinkTaskKafkaCassandra(sinkableType, request))))(makeResult)
        }
      }
    }


  val getRoute = pathPrefix("sink") {
    get {
      onComplete(actorRef ? Sink(GetTasks))(makeResult)
    }
  }


}