package model

import java.util.UUID
import akka.actor.ActorRef

object RestApi {

  trait Action
  case object Start extends Action
  case object Stop extends Action
  case object GetTasks extends Action


  trait Requests
  case class SinkParams(kafkaTopic: String, cassandraKeyspace:String, cassandraTable:String) extends Requests

  trait SinkTask {
    def getDescription: String
  }
  // TODO: Use Generic (t: SinkableType)
  case class SinkTaskKafkaCassandra(t: SinkableType, params: SinkParams) extends SinkTask {
    override def getDescription: String = s"SinkableType: $t, SinkParams: $params"
  }
  trait SinkRequests extends Requests
  case class Sink(action: Action, sinkTask: Option[SinkTask] = None) extends SinkRequests
  case class SinkJob(actorRef: ActorRef, sink: SinkRequests) extends SinkRequests

  trait StreamRequests extends Requests
  case class StreamTask(backpressuredActorRef: Option[ActorRef], user: UUID, fileName: String, folderName: String, destinationType: Int) extends StreamRequests
  case class StreamJob(actorRef: ActorRef, streamTask: StreamTask) extends StreamRequests

  trait FileSenderRequests extends Requests
  case class FileSenderTask(fileSenderActor: ActorRef, fileName: String) extends FileSenderRequests
  case class FileSenderJob(actorRef: ActorRef, fileSenderTask: FileSenderTask) extends FileSenderRequests

  trait DbRequests extends Requests
  case class DbRequestData(actorRef: ActorRef, request: DbRequests)
  case class DbJob(actorRef: ActorRef, requestBody: DbRequests) extends DbRequests
  case class GetFileNamesQL(queryText: String) extends DbRequests



  trait Responces

  trait Err extends Responces
  case object Ok extends Responces
  case object UnhandledDbTask extends Err
  case class AnyErr(message: String) extends Err

  trait SinkTaskResponces
  case object UnhandledSinkTask extends Err
  case class SinkTasks(tasksDescription: Seq[String]) extends SinkTaskResponces
  case object SinkTasksAlreadyInProgress extends SinkTaskResponces

  trait StreamTaskResponces
  case object Ping extends StreamTaskResponces
  case object ChunkAccepted extends StreamTaskResponces
  case object ChunkDenied extends StreamTaskResponces

  trait DBResponces extends Responces
  case class FileNamesQL(fileNamesQL: Seq[FileNameGraphQL]) extends DBResponces

}