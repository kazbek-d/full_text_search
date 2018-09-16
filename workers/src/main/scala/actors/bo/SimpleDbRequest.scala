package actors.bo

import akka.actor.{Actor, ActorLogging}
import kamon.trace.Tracer
import model.RestApi.{DbRequestData, GetFileNamesQL}
import common.SparkImplicits.repositoryCassyJavaDriver

class SimpleDbRequest extends Actor with ActorLogging {

  override def receive: Receive = {

    case DbRequestData(actorRef, GetFileNamesQL(queryText)) =>
      Tracer.withNewContext("GetFileNamesQL comes", autoFinish = true) {
        log.info("GetFileNamesQL comes.")
        actorRef ! repositoryCassyJavaDriver.getFileNames(repositoryCassyJavaDriver.luceneChunksText(queryText))
      }

    case _ =>
      Tracer.withNewContext("Something comes", autoFinish = true) {
        log.info("Something comes.")
      }
  }

}
