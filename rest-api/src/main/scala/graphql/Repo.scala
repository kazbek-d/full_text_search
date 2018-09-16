package graphql

import java.util.UUID
import akka.pattern.ask
import akka.actor.ActorRef
import akka.util.Timeout
import model.FileName
import model.RestApi.{FileNamesQL, GetFileNamesQL}
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class Repo(actorRef: ActorRef) {

  def getFilesFake(name: String): Future[Option[FileName]] =
    Future {
      Some(FileName(new UUID(0L, 0L), "Fake File", "Fake Dir", 0, Map.empty, 1000, 1))
    }


  val timeout10Minutes = 10.minute
  implicit val timeout: Timeout = timeout10Minutes

  def getFileNamesQL(queryText: String): Future[FileNamesQL] =
    (actorRef ? GetFileNamesQL(queryText)).map {
      case fileNamesQL: FileNamesQL => fileNamesQL
      case ex@_ => throw new FileIOError(ex.toString)
    }

}
