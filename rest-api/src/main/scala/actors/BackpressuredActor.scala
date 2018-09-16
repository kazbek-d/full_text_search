package actors

import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import model.Chunk
import model.RestApi.{ChunkAccepted, ChunkDenied}

import scala.annotation.tailrec
import scala.language.postfixOps


class BackpressuredActor extends ActorPublisher[Chunk] with ActorLogging {

  val MaxBufferSize = 100
  var buffer = Vector.empty[Chunk]
  implicit val ec = context.dispatcher

  @tailrec
  private def deliverBuffer(): Unit =
    if (totalDemand > 0 && isActive) {
      // You are allowed to send as many elements as have been requested by the stream subscriber
      // total demand is a Long and can be larger than what the buffer has
      if (totalDemand <= Int.MaxValue) {
        val (sendDownstream, holdOn) = buffer.splitAt(totalDemand.toInt)
        buffer = holdOn
        // send the stuff downstream
        sendDownstream.foreach(onNext)
      } else {
        val (sendDownStream, holdOn) = buffer.splitAt(Int.MaxValue)
        buffer = holdOn
        sendDownStream.foreach(onNext)
        // recursive call checks whether is more demand before repeating the process
        deliverBuffer()
      }
    }

  override def receive: Receive = {

    case _: Chunk if buffer.size == MaxBufferSize =>
      log.warning("received a Chunk message when the buffer is maxed out")
      sender() ! ChunkDenied

    case chunk: Chunk =>
      log.info("Chunk comes")
      sender() ! ChunkAccepted
      if (buffer.isEmpty && totalDemand > 0 && isActive) {
        // send elements to the stream immediately since there is demand from downstream and we
        // have not buffered anything so why bother buffering, send immediately
        // You send elements to the stream by calling onNext
        onNext(chunk)
      }
      else {
        // there is no demand from downstream so we will store the result in our buffer
        // Note that :+= means add to end of Vector
        buffer :+= chunk
      }

    // A request from down stream to send more data
    // When the stream subscriber requests more elements the ActorPublisherMessage.Request message is
    // delivered to this actor, and you can act on that event. The totalDemand is updated automatically.
    case Request(_) =>
      deliverBuffer()

    // When the stream subscriber cancels the subscription the ActorPublisherMessage.Cancel message is
    // delivered to this actor. If the actor is stopped the stream will be completed, unless it was not
    // already terminated with failure, completed or canceled.
    case Cancel =>
      log.info("BackpressuredActor was cancelled")
      context.stop(self)

    case other =>
      log.warning(s"Unknown message $other received")
  }

}