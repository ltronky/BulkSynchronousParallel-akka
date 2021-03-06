package it.unipd.trluca.bsp.engine.aggregators

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection}
import akka.contrib.pattern.Aggregator
import it.unipd.trluca.bsp.engine.{Active, Message, ResReceived}

import scala.collection.mutable.ArrayBuffer

case class DispatchMessage(agents:ArrayBuffer[ActorSelection], phase:Int, m:Any)

class MessageDispatcher extends Actor with Aggregator with ActorLogging {

  val results = ArrayBuffer.empty[Unit]
  var originalSender:ActorRef = null
  var agentSize:Int = 0

  expectOnce {
    case DispatchMessage(agents, phase, m) =>
      originalSender = sender()
      agentSize = agents.size
      agents foreach { a =>
        a ! Message(phase, m)
      }
  }

  val handle = expect {
    case ResReceived =>
      results += ResReceived
      if (results.size >= agentSize) processResult()
  }

  def processResult() {
    unexpect(handle)
    originalSender ! Active(s = true)
    context.stop(self)
  }

}
