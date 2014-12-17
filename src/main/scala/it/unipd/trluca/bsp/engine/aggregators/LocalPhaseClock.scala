package it.unipd.trluca.bsp.engine.aggregators

import akka.actor.{Actor, ActorRef}
import akka.contrib.pattern.Aggregator
import it.unipd.trluca.bsp._
import it.unipd.trluca.bsp.engine.PhaseTerminated

import scala.collection.mutable.ArrayBuffer

case class ExecutePhaseLocal(localAgents:Array[ActorRef], phase:Int)

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
