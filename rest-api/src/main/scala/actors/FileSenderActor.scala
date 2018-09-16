package actors

import java.io.{File, FileInputStream}
import java.nio.file.Files
import java.util.UUID

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.stream.ActorAttributes
import akka.stream.actor.ActorPublisherMessage.Cancel
import akka.stream.scaladsl.FileIO._
import common.Adler
import common.AkkaImplicits._
import model.RestApi._
import model.{Chunk, FileName}

import scala.language.postfixOps
import scala.util.{Failure, Success}


class FileSenderActor extends Actor with ActorLogging {

  private var streamIterator: Iterator[Chunk] = _
  private var actorRef: ActorRef = _
  private var currentChunk: Option[Chunk] = None

  private var userId: UUID = _
  private var fileName: String = _
  private var folderName: String = _
  private var destination_type: Int = _
  private var arrayBuffer = List[(Int,UUID)]()
  private var content_length: Long = _

  def sendNextChunk = if(destination_type == FileName.saveToDataBase || destination_type == FileName.saveToDataBaseAndInTextFormat) {
    if (streamIterator.isEmpty)
      currentChunk = None
    else {
      val temp = currentChunk
      currentChunk = Option(streamIterator.next())
      def addItem(chunk: Chunk) = arrayBuffer = (arrayBuffer.headOption.map(_._1 + 1).getOrElse(0) -> chunk.pk) :: arrayBuffer
      temp match {
        case None => currentChunk.foreach(addItem)
        case _ => temp.flatMap(t => currentChunk.withFilter(c => t.pk != c.pk).map(addItem))
      }
    }
    sendData
  } else {
    actorRef ! FileName(userId, fileName, folderName, destination_type, Map.empty, 0, 0)
    context.stop(self)
  }

  def sendData =
    currentChunk match {
      case Some(chunk) =>
        content_length += chunk.length
        actorRef ! (if (destination_type == FileName.saveToDataBaseAndInTextFormat) chunk.toChunkText else chunk)
      case None =>
        log.info(s"context.stop(self)")
        actorRef ! FileName(userId, fileName, folderName, destination_type, arrayBuffer.toMap, content_length, adler.getAdler32sum)
        context.stop(self)
    }

  val adler = new Adler
  val chunkMult = 64
  val chunkSize = 1024 * chunkMult
  val chunkCount = 1024 / chunkMult

  override def receive: Receive = {

    case (file: File, user: UUID, fileInfo: String, folderInfo: String, destinationType: Int) =>
      log.info(s"File comes.")

      userId = user
      fileName = fileInfo
      folderName = folderInfo
      destination_type = destinationType

      def toFsCassy(senderRef: Option[ActorRef]) : Unit = {
        val fileInputStream = new FileInputStream(file)
        def fileContentStream(pk: UUID, index: Int, fileIn: FileInputStream): Stream[Chunk] = {
          val currIndex = if (index > chunkCount) 0 else index
          val currPK = if (index > chunkCount) UUID.randomUUID() else pk
          val bytes = Array.fill[Byte](chunkSize)(0)
          val length = fileIn.read(bytes)
          bytes take length foreach adler.processAdler32sum
          Chunk(currPK, currIndex, length, bytes) #:: fileContentStream(currPK, currIndex + 1, fileIn)
        }

        streamIterator = {
          fileContentStream(UUID.randomUUID(), 0, fileInputStream) takeWhile { chunk => chunk.length > 0 } iterator
        }

        senderRef.foreach(_ ! Done) // Send Done to RestActor
      }

      def toFs(senderRef: ActorRef): Unit = {
        val dir = FileName.dir(userId, folderName)
        Files.createDirectories(dir)
        val path = FileName.path(dir, fileName)
        val source = fromPath(file.toPath)
        val sink = toPath(path)
        source
          .withAttributes(ActorAttributes.dispatcher("akka.stream.default-blocking-io-dispatcher"))
          .to(sink)
          .run()
          .onComplete {
            case Success(value) => value.status match {
              case Failure(exception) =>
                senderRef ! AnyErr(exception.getMessage) // Send Err to RestActor
              case Success(_) =>
                senderRef ! Done // Send Done to RestActor
            }
            case Failure(ex) =>
              senderRef ! AnyErr(ex.getMessage) // Send Err to RestActor
          }
      }

      if(destinationType == FileName.saveToDataBase)
        toFsCassy(Some(sender))
      else if(destinationType == FileName.saveToDataBaseAndInTextFormat) {
        toFsCassy(None)
        toFs(sender)
      } else if(destinationType == FileName.saveToFileSystem)
        toFs(sender)

      log.info(s"sender() ! Done.")

    case fileReceiverActor: ActorRef =>
      log.info(s"FileReceiverActor comes.")
      actorRef = fileReceiverActor
      sendNextChunk

    case ChunkDenied =>
      log.info(s"ChunkDenied comes.")
      sendData

    case ChunkAccepted =>
      log.info(s"ChunkAccepted comes.")
      sendNextChunk

    case Cancel =>
      log.info("FileSenderActor was cancelled")
      context.stop(self)

  }

}