import actors.{BackendClusterListener, RemoteSink}
import akka.actor._
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import common.Settings._
import kamon.Kamon
import kamon.trace.Tracer

import scala.concurrent.Await
import scala.concurrent.duration.Duration



// export WORKER_PORT = 2771
// sbt run java -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false
// ./akka-cluster 127.0.0.1 9999 cluster-status
// akka-cluster 127.0.0.1 9999 down akka.tcp://FileIOClusterSystem@127.0.0.1:49603
// sbt aspectj-runner:run -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false
// java -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar backend-workers-assembly-1.0.jar

object Worker extends App {

  Kamon.start()

  import common.AkkaImplicits._

  system.actorOf(Props[BackendClusterListener], actorBackendName)

  system.actorOf(
    ClusterSingletonManager.props(
      singletonProps = Props[RemoteSink],
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(system).withRole(actorBackendName)
    ),
    name = "remote-sink")

//  Tracer.withNewContext("TraceNameThatRepresentTheCodeExecuted", autoFinish = true) {
//    // code to trace
//  }
  //  val someHistogram = Kamon.metrics.histogram("some-histogram")
  //  val someCounter = Kamon.metrics.counter("some-counter")
  //
  //  someHistogram.record(42)
  //  someHistogram.record(50)
  //  someCounter.increment()

  Await.result(system.whenTerminated, Duration.Inf)

}