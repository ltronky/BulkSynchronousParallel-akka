package it.unipd.trluca.bsp.aggregators

import akka.actor.{Actor, ActorRef}
import akka.cluster.Cluster
import akka.contrib.pattern.Aggregator
import it.unipd.trluca.bsp.{Done, EngineStep, ConstStr}

import scala.collection.mutable.ArrayBuffer

class WorldClock extends Actor with Aggregator {
  val results = ArrayBuffer.empty[Unit]
  var originalSender:ActorRef = null
  val members = Cluster(context.system).state.members

  expectOnce {
    case es:EngineStep =>
      originalSender = sender()
      members foreach { m =>
        context.actorSelection(m.address + ConstStr.NODE_ACT_NAME) ! es
      }
  }

  val handle = expect {
    case Done =>
      results += Done
      if (results.size >= members.size) processResult()
  }

  def processResult() {
    unexpect(handle)
    originalSender ! Done
    context.stop(self)
  }

}
