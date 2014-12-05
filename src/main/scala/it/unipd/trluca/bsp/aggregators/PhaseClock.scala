package it.unipd.trluca.bsp.aggregators

import akka.actor.{Actor, ActorRef}
import akka.cluster.Cluster
import akka.contrib.pattern.Aggregator
import it.unipd.trluca.bsp._

import scala.collection.mutable.ArrayBuffer

class PhaseClock extends Actor with Aggregator {
  val results = ArrayBuffer.empty[PhaseTerminated]
  var originalSender:ActorRef = null
  val members = Cluster(context.system).state.members

  expectOnce {
    case ep:ExecutePhase =>
      originalSender = sender()
      members foreach { m =>
        context.actorSelection(m.address + ConstStr.NODE_ACT_NAME) ! ep
      }
  }

  val handle = expect {
    case PhaseTerminated(agentsAlive) =>
      results += PhaseTerminated(agentsAlive)
      if (results.size >= members.size) processResult()
  }

  def processResult() {
    unexpect(handle)
    originalSender ! results.reduce((x, y) => PhaseTerminated(x.agentsAlive + y.agentsAlive))
    context.stop(self)
  }

}
