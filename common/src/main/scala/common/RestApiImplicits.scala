package common

import model.FileNameGraphQL
import model.RestApi._
import spray.json.DefaultJsonProtocol._

object RestApiImplicits {

  implicit val anyErrFormat = jsonFormat1(AnyErr)
  implicit val SinkParamsFormat = jsonFormat3(SinkParams)
  implicit val SinkTasksFormat = jsonFormat1(SinkTasks)

  implicit val FileNameGraphQLFormat = jsonFormat3(FileNameGraphQL)
  implicit val FileNamesQLFormat = jsonFormat1(FileNamesQL)

}
