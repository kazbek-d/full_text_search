package routs

import akka.Done
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}

import scala.util.{Failure, Success, Try}
import model.RestApi._

trait BaseRout {

  import common.RestApiImplicits._

  protected val makeResult: (Try[Any]) => StandardRoute = {
    case Success(value) => value match {
      case err: AnyErr => complete(StatusCodes.BadRequest, err)

      case sinkTasks : SinkTasks => complete(StatusCodes.Accepted, sinkTasks)
      case SinkTasksAlreadyInProgress => complete(StatusCodes.Accepted, SinkTasksAlreadyInProgress.toString)

      case Done => complete(StatusCodes.Accepted, "Done")

      case _ => complete(StatusCodes.Accepted, "Ok")
    }
    case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
  }

  def getRoute: Route

}

