package actors

import akka.actor.Props
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.routing.RoundRobinPool
import common.AkkaImplicits._

object EndWorkers {

  lazy val dbActor = system.actorOf(
    Props(new DbActor).withRouter(RoundRobinPool(nrOfInstances = 100)), "dbActor")

  import common.Settings._

  val remoteSink = system.actorOf(
    ClusterSingletonProxy.props(
      singletonManagerPath = "/user/remote-sink",
      settings = ClusterSingletonProxySettings(system).withRole(actorBackendName)
    ),
    name = "remote-sink-proxy"
  )

}
