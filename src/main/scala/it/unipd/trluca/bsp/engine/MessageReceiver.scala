package it.unipd.trluca.bsp.engine

import akka.actor.{Actor, ActorLogging}

import scala.collection.mutable.ArrayBuffer


case class GetInbox(phase:Int)
case object ResReceived
case class Message(phase:Int, x:Any)

class MessageReceiver[S] extends Actor with ActorLogging {
  val inc = Array(ArrayBuffer.empty[Any], ArrayBuffer.empty[Any])

  def receive = {
    case Message(phase, x) => inc(phase%2) += x
      sender() ! ResReceived

    case GetInbox(phase) =>
      sender() ! inc(phase%2).toArray
      inc(phase%2).clear()

    case _=> log.info("Messaggio ignorato MessageReceiver")
  }

}
