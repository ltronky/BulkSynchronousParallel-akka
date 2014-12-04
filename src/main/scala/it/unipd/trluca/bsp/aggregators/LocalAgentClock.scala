package it.unipd.trluca.bsp.aggregators

import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.cluster.Cluster
import akka.contrib.pattern.Aggregator
import it.unipd.trluca.bsp._

import scala.collection.mutable.ArrayBuffer

class LocalAgentClock extends Actor with Aggregator with ActorLogging {
  val results = ArrayBuffer.empty[Unit]
  var originalSender:ActorRef = null
  var size = 0

  expectOnce {
    case cac:CreateAgentConn =>
      originalSender = sender()
      size = cac.localAgents.size

      cac.localAgents foreach { _ ! cac }

    case pr:PrintResultLocal =>
      originalSender = sender()
      size = pr.localAgents.size
      pr.localAgents foreach { _ ! PrintResult }
  }

  val handle = expect {
    case Done =>
      results += Done
      if (results.size >= size) processResult()
  }

  def processResult() {
    unexpect(handle)
    originalSender ! Done
    context.stop(self)
  }

}
