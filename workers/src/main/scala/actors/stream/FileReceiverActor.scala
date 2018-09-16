package actors.stream

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef}
import model.RestApi.{ChunkAccepted, FileSenderJob}
import model.{Chunk, ChunkText, FileName}


class FileReceiverActor extends Actor with ActorLogging {

  import common.SparkImplicits._

  var restApiActor: ActorRef = _
  var fileName: String = _


  override def receive: Receive = {

    case request: FileSenderJob =>
      log.info(s"FileSenderJob comes.")
      restApiActor = request.actorRef
      fileName = request.fileSenderTask.fileName
      //println(s"FileName: $fileName")
      request.fileSenderTask.fileSenderActor ! self

    case chunk: Chunk =>
      log.info("Chunk comes")
      //println(chunk)
      repositoryCassyJavaDriver.setChunk(chunk)
      sender() ! ChunkAccepted

    case chunkText: ChunkText =>
      log.info("ChunkText comes")
      //println(chunk)
      repositoryCassyJavaDriver.setChunkText(chunkText)
      sender() ! ChunkAccepted

    case fileName: FileName =>
      log.info("FileReceiverActor was cancelled")
      //println(fileName)
      fileName.toCassandraable.foreach(repositoryCassyJavaDriver.setFileName)
      restApiActor ! Done // Send Done to RestActor
      context.stop(self)

  }

}
