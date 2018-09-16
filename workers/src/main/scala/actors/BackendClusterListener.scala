package actors


import actors.EndWorkers.{dbActor, remoteSink}
import actors.stream.{FileReceiverActor, FileSenderActor}
import akka.actor.{Actor, ActorLogging, Props, RootActorPath}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, Member, MemberStatus}
import common.AkkaImplicits.system
import common.Settings._
import model.AkkaObjects.BackendRegistration
import model.RestApi._

class BackendClusterListener extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive = {

    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberUp(member) =>
      log.info(s"[Listener] node is up: $member")
      register(member)


    case request: DbJob =>
      log.info(s"DataBase job comes.")
      dbActor ! request

    case request: SinkJob =>
      log.info(s"Sink job comes for Actor: $remoteSink")
      remoteSink ! request

    case request: StreamJob =>
      log.info(s"StreamJob job comes.")
      system.actorOf(Props[FileSenderActor]) ! request

    case request: FileSenderJob =>
      log.info(s"FileSenderJob job comes.")
      system.actorOf(Props[FileReceiverActor]) ! request


  }

  def register(member: Member): Unit =
    if (member.hasRole("frontend"))
      context.actorSelection(RootActorPath(member.address) / "user" / actorFrontendName) !
        BackendRegistration

}
