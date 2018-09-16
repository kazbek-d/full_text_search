package routs

import java.util.UUID

import actors.FileSenderActor
import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorAttributes
import akka.stream.scaladsl.FileIO.fromPath
import akka.util.Timeout
import common.AkkaImplicits._
import model.FileName
import model.RestApi.{FileSenderTask, StreamTask}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class FileRout(actorRef: ActorRef)  extends BaseRout {

  implicit val timeout: Timeout = 10.minute


  val getRoute = pathPrefix("file") {

    path("load" / RemainingPath) { path =>
      // wget "http://localhost:8112/file/load/fileName"
      get {
        val fileName = path.toString()
        onComplete(actorRef ? StreamTask(None, new UUID(0L, 0L), fileName, "", FileName.saveToFileSystem)) {
          case Success(value) => value match {
            case _: FileName =>
              val path = FileName.path(FileName.dir(new UUID(0L, 0L), ""), fileName)
              val source = fromPath(path)
                .withAttributes(ActorAttributes.dispatcher("akka.stream.default-blocking-io-dispatcher"))
              val file = path.toFile
              if (file.exists)
                complete(HttpResponse(entity = HttpEntity.Default(ContentTypes.`application/octet-stream`, file.length, source)))
              else
                complete(StatusCodes.NoContent, "File not fount")
            case _ => complete(StatusCodes.NoContent, "File not fount")
          }
          case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    } ~
      uploadedFile("csv") {
        // curl --form "csv=@fileName" http://localhost:8112/file
        case (fileInfo, file) =>
          onComplete {
            val fileSenderActor = system.actorOf(Props[FileSenderActor])
            (fileSenderActor ? (file, new UUID(0L, 0L), fileInfo.fileName, "", FileName.saveToFileSystem)).flatMap(_ => actorRef ? FileSenderTask(fileSenderActor, fileInfo.fileName))
          }(makeResult)
      }

  }

}



