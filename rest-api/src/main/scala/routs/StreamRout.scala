package routs

import java.util.UUID

import actors.{BackpressuredActor, FileSenderActor}
import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}
import common.AkkaImplicits._
import model.RestApi.{FileSenderTask, StreamTask}
import model.{Chunk, FileName}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class StreamRout(actorRef: ActorRef)  extends BaseRout {

  implicit val timeout: Timeout = 10.minute


  val getRoute = pathPrefix("stream") {

    path("load" / RemainingPath) { path =>
      // wget "http://localhost:8112/stream/load/fileName"
      get {
        val fileName = path.toString()
        val backpressuredActorRef = system.actorOf(Props[BackpressuredActor])
        val publisher = ActorPublisher[Chunk](backpressuredActorRef)
        val source = Source.fromPublisher(publisher).map(chunk => ByteString(chunk.bytes.take(chunk.length)))
        onComplete(actorRef ? StreamTask(Some(backpressuredActorRef), new UUID(0L, 0L), fileName, "", FileName.saveToDataBase)) {
          case Success(value) => value match {
            case FileName(_, _, _, _, _, content_length, _) =>
              complete {
                HttpResponse(entity = HttpEntity.Default(ContentTypes.`application/octet-stream`, content_length, source))
              }
            case _ => complete(StatusCodes.NoContent, "File not fount")
          }
          case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
        }

      }
    } ~
      uploadedFile("csv") {
        // curl --form "csv=@fileName" http://localhost:8112/stream
        case (fileInfo, file) =>
          onComplete {
            val fileSenderActor = system.actorOf(Props[FileSenderActor])
            (fileSenderActor ? (file, new UUID(0L, 0L), fileInfo.fileName, "", FileName.saveToDataBase)).flatMap(_ => actorRef ? FileSenderTask(fileSenderActor, fileInfo.fileName))
          }(makeResult)
      }

  }

}



