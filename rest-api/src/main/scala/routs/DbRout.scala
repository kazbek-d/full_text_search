package routs

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import kamon.trace.Tracer

import scala.concurrent.duration._


class DbRout(actorRef: ActorRef) extends BaseRout {

  import common.RestApiImplicits._

  val timeout10Minutes = 10.minute
  implicit val timeout: Timeout = timeout10Minutes

  val getRoute = pathPrefix("db") {

    path("test") {
      get {
        getFromResource("graphiql.html")
      }
    }

  }

}