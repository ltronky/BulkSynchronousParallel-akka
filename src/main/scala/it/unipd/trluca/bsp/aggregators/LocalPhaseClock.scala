package it.unipd.trluca.bsp.aggregators

import akka.actor.{Actor, ActorRef}
import akka.contrib.pattern.Aggregator
import it.unipd.trluca.bsp._

import scala.collection.mutable.ArrayBuffer

class LocalPhaseClock extends Actor with Aggregator {
  val results = ArrayBuffer.empty[PhaseTerminated]
  var originalSender:ActorRef = null
  var size = 0

  expectOnce {
    case ep:ExecutePhaseLocal =>
      originalSender = sender()
      size = ep.localAgents.size
      ep.localAgents foreach { _ ! ep }
  }

  val handle = expect {
    case pt:PhaseTerminated =>
      results += pt
      if (results.size >= size) processResult()
  }

  def processResult() {
    unexpect(handle)
    originalSender ! results.reduce((x, y) => PhaseTerminated(x.agentsAlive + y.agentsAlive))
    context.stop(self)
  }

}
