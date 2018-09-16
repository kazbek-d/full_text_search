import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import routs._
import actors.FrontendClusterListener
import common.Settings._
import kamon.Kamon


object WebServer extends App {

  Kamon.start()

  import common.AkkaImplicits._
  val frontendActor = system.actorOf(Props(new FrontendClusterListener), actorFrontendName)

  val sinkTasks = new SinkRout(frontendActor)
  val db = new DbRout(frontendActor)
  val ql = new QLRout(frontendActor)
  val st = new StreamRout(frontendActor)
  val stext = new StreamExtRout(frontendActor)
  val fs = new FileRout(frontendActor)
  val route = sinkTasks.getRoute ~ db.getRoute ~ st.getRoute ~ stext.getRoute ~ fs.getRoute ~ ql.getRoute

  Http().bindAndHandle(route, webserverAddress, webserverPort)

  Await.result(system.whenTerminated, Duration.Inf)

}