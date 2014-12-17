package it.unipd.trluca.bsp.engine.aggregators

import akka.actor.{ActorSelection, Actor, ActorRef}
import akka.contrib.pattern.Aggregator
import it.unipd.trluca.bsp._
import it.unipd.trluca.bsp.engine.{ResReceived, Message}

import scala.collection.mutable.ArrayBuffer

case class StartAgents(localAgents:List[ActorSelection], m:Message)

class InitStartAgentsAggregator extends Actor with Aggregator {
  val results = ArrayBuffer.empty[Unit]
  var originalSender:ActorRef = null
  var size = 0

  expectOnce {
    case ep:StartAgents =>
      originalSender = sender()
      size = ep.localAgents.size
      ep.localAgents foreach { _ ! ep.m }
  }

  val handle = expect {
    case ResReceived =>
      results += ResReceived
      if (results.size >= size) processResult()
  }

  def processResult() {
    unexpect(handle)
    originalSender ! Done
    context.stop(self)
  }

}
