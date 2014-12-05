package it.unipd.trluca.bsp

import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import akka.cluster.Member
import akka.pattern.ask
import it.unipd.trluca.bsp.aggregators.{LocalPhaseClock, LocalAgentClock}

import scala.collection.SortedSet
import scala.concurrent.ExecutionContext.Implicits.global

case class CreateAgentConn(localAgents:Array[ActorRef], clusterMembers:SortedSet[Member], nr:Int)
case class ExecutePhaseLocal(localAgents:Array[ActorRef], phase:Int)
case class PrintResultLocal(localAgents:Array[ActorRef])

class NodeRootActor extends Actor with ActorLogging {
  implicit val timeout = ConstStr.MAIN_TIMEOUT

  var localAgents:Array[ActorRef] = null


  def receive = {
    case InitNode(size) =>
      localAgents = Array.tabulate(size) {i => context.actorOf(V.props(i), s"ag$i")}
      sender() ! Done

    case CreateConnections(clusterMembers, nr) =>
      val orSender = sender()
      val wc = context.actorOf(Props[LocalAgentClock])
      val response = wc ? CreateAgentConn(localAgents, clusterMembers, nr)
      response map { Done =>
        orSender ! Done
      }

    case ExecutePhase(phase) => //TODO spostare su Engine Part
      val orSender = sender()
      val wc = context.actorOf(Props[LocalPhaseClock])
      val response = (wc ? ExecutePhaseLocal(localAgents, phase)).mapTo[PhaseTerminated]
      response map { pt:PhaseTerminated =>
        orSender ! pt
      }

    case PrintResult =>
      val orSender = sender()
      val wc = context.actorOf(Props[LocalAgentClock])
      val response = wc ? PrintResultLocal(localAgents)
      response map { Done =>
        orSender ! Done
      }

    case TakeDownCluster =>
      context.system.shutdown()

    case _=>
  }
}
