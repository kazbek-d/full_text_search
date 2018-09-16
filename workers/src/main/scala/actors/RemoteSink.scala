package actors

import actors.sink.CassandraSink
import akka.actor._
import model.RestApi._

class RemoteSink extends Actor with ActorLogging {


  private var accMap = scala.collection.mutable.HashMap.empty[SinkTask, ActorRef]

  private def getChild(sinkTask: SinkTask): Option[ActorRef] = accMap.get(sinkTask)

  override def receive: Receive = {

    case job: SinkJob =>
      log.info(s"SinkJob comes to $self from ${job.actorRef}!")

      job.sink match {
        case Sink(action, sinkTask) =>
          action match {
            case Start if sinkTask.isDefined =>
              sinkTask.foreach(task => {
                getChild(task) match {
                  case Some(_) => job.actorRef ! SinkTasksAlreadyInProgress
                  case _ => task match {
                    case params: SinkTaskKafkaCassandra =>
                      accMap += (task -> context.system.actorOf(Props(new CassandraSink(params)), task.getDescription.hashCode.toString))
                      job.actorRef ! Ok
                    case _ => job.actorRef ! UnhandledSinkTask
                  }
                }
              })
            case Stop if sinkTask.isDefined =>
              sinkTask.foreach(task => {
                getChild(task).foreach(_ ! PoisonPill)
                accMap.remove(task)
              })
              job.actorRef ! Ok
            case GetTasks =>
              job.actorRef ! SinkTasks(accMap.keySet.toSeq.map(_.getDescription))
            case _ =>
              job.actorRef ! UnhandledSinkTask
          }
        case _ =>
          job.actorRef ! UnhandledSinkTask
      }

  }

}
