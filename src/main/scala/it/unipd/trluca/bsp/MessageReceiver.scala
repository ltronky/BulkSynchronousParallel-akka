package it.unipd.trluca.bsp

import akka.actor.{ActorLogging, Actor}

import scala.collection.mutable.ArrayBuffer


case object GetInbox
case object ResReceived
case class Message(x:Any)

class MessageReceiver[S] extends Actor with ActorLogging {
  var inc:ArrayBuffer[Any] = ArrayBuffer.empty[Any]

  def receive = {
    case Message(x) => inc += x
      sender() ! ResReceived

    case GetInbox =>
      sender() ! inc.toArray
      inc.clear()
    case _=> log.info("Messaggio ignorato MessageReceiver")
  }

}
