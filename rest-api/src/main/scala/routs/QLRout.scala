package routs

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import common.AkkaImplicits._
import graphql.{QueryDefinition => qd, SchemaDefinition => sd, _}
import kamon.trace.Tracer
import sangria.execution.deferred.DeferredResolver
import sangria.execution.{ErrorWithResolver, Executor, HandledException, QueryAnalysisError}
import sangria.marshalling.ResultMarshaller
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import spray.json._

import scala.util.{Failure, Success}

class QLRout(actorRef: ActorRef) {

  val getRoute =
    (post & path("graphql")) {
      entity(as[JsValue]) { requestJson ⇒
        val JsObject(fields) = requestJson

        val JsString(query) = fields("query")

        val operation = fields.get("operationName") collect {
          case JsString(op) ⇒ op
        }

        val vars = fields.get("variables") match {
          case Some(obj: JsObject) ⇒ obj
          case _ ⇒ JsObject.empty
        }

        val exceptionHandler = sangria.execution ExceptionHandler(
          PartialFunction[(ResultMarshaller, Throwable), HandledException] {
            case (_, e: FileIOError) ⇒ HandledException(e.getMessage)
            case (_, e: Throwable) ⇒ throw e
          },
          PartialFunction.empty,
          PartialFunction.empty
        )

        QueryParser.parse(query) match {

          // query parsed successfully, time to execute it!
          case Success(queryAst) ⇒
            Tracer.withNewContext("graphql___query", autoFinish = true) {
              complete {
                Executor.execute(
                  schema = sd.fileIOSchema,
                  queryAst = queryAst,
                  userContext = new Repo(actorRef),
                  variables = vars,
                  operationName = operation,
                  deferredResolver = DeferredResolver.fetchers(qd.filesFetcherCaching),
                  exceptionHandler = exceptionHandler
                ).map(OK → _)
                  .recover {
                    case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
                    case error: ErrorWithResolver ⇒ InternalServerError → error.resolveError
                  }
              }
            }

          // can't parse GraphQL query, return error
          case Failure(error) ⇒
            complete(BadRequest, JsObject("error" → JsString(error.getMessage)))
        }
      }
    } ~
      get {
        getFromResource("graphiql.html")
      }

}