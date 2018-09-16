package actors

import actors.bo.SimpleDbRequest
import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import kamon.trace.Tracer
import model.RestApi._

import scala.concurrent.duration._

class DbActor extends Actor with ActorLogging {

  implicit val timeout: Timeout = 1.minute

  private val simpleDbRequestActor = context.system.actorOf(Props(new SimpleDbRequest))

  override def receive = {

    case job: DbJob =>
      Tracer.withNewContext("DbJob", autoFinish = true) {
        log.info(s"DbJob comes.")
        job.requestBody match {
          case request: DbRequests => simpleDbRequestActor ! DbRequestData(job.actorRef, request)
          case _ => job.actorRef ! UnhandledDbTask
        }
      }

  }

}