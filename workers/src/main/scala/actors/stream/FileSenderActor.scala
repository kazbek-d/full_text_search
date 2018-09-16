package actors.stream

import java.util.UUID

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.stream.actor.ActorPublisherMessage.Cancel
import data.CassandraRepositoryJavaDriver
import model.{Chunk, FileName}
import model.RestApi._


class FileSenderActor extends Actor with ActorLogging {

  val repository = new CassandraRepositoryJavaDriver

  var backpressuredActorRef: ActorRef = _
  var pks: Map[Int, UUID] = _
  var pkIndex: Int = _
  var pkIndexPrev: Int = _
  var chunks: Map[Int, Chunk] = _
  var chunkIndex: Int = _

  def sendData : Unit =
    pks.get(pkIndex) match {
      case Some(pk) =>
        if(pkIndex != pkIndexPrev) {
          chunks = repository.getChunk(pk).map(x=>(x.chunk_index, x)).toMap
          pkIndexPrev = pkIndex
          chunkIndex = 0
        }
        chunks.get(chunkIndex) match {
          case Some(chunk) =>
            backpressuredActorRef ! chunk
          case None =>
            pkIndex = pkIndex + 1
            sendData
        }
      case None =>
        backpressuredActorRef ! Cancel
        context.stop(self)
    }


  override def receive: Receive = {

    case StreamJob(restActorRef: ActorRef, streamTask: StreamTask) =>
      log.info(s"StreamTask comes.")
      val fileName = repository.getFileName(streamTask.user, streamTask.fileName, streamTask.folderName, streamTask.destinationType)
      if(streamTask.destinationType == FileName.saveToDataBase) {
        if (fileName.isEmpty) {
          restActorRef ! Done
          streamTask.backpressuredActorRef.get ! Cancel
          context.stop(self)
        } else {
          pks = fileName.map(x => (x.chunk_pk_index, x.chunk_pk)).toMap
          restActorRef ! fileName.head.toFileNameMetaData
          backpressuredActorRef = streamTask.backpressuredActorRef.get
          pkIndex = 0
          chunkIndex = 0
          pkIndexPrev = -1
          sendData
        }
      } else if(streamTask.destinationType == FileName.saveToFileSystem || streamTask.destinationType == FileName.saveToDataBaseAndInTextFormat) {
        restActorRef ! (if (fileName.isEmpty) AnyErr("File not found") else fileName.head.toFileNameMetaData)
        context.stop(self)
      }


    case ChunkDenied =>
      log.info(s"StreamTask comes.")
      sendData

    case ChunkAccepted =>
      log.info(s"StreamTask comes.")
      chunkIndex += 1
      sendData

    case Cancel =>
      log.info("StreamActor was cancelled")
      context.stop(self)

  }
}