package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import model.AkkaObjects.BackendRegistration
import model.RestApi._


class FrontendClusterListener extends Actor with ActorLogging {

  var jobCounter = 0
  var backends = IndexedSeq.empty[ActorRef]

  override def receive = {

    case BackendRegistration if !backends.contains(sender()) =>
      context watch sender()
      backends = backends :+ sender()

    case Terminated(actorRef) =>
      backends = backends.filterNot(_ == actorRef)


    case _: Requests if backends.isEmpty =>
      sender ! AnyErr("Service unavailable, try again later")


    case request: DbRequests =>
      jobCounter += 1
      backends(jobCounter % backends.size) forward DbJob(sender, request)

    case request: SinkRequests =>
      jobCounter += 1
      backends(jobCounter % backends.size) forward SinkJob(sender, request)

    case request: StreamTask =>
      jobCounter += 1
      backends(jobCounter % backends.size) forward StreamJob(sender, request)

    case request: FileSenderTask =>
      jobCounter += 1
      backends(jobCounter % backends.size) forward FileSenderJob(sender, request)


  }
}
