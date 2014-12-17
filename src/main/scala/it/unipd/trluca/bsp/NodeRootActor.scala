package it.unipd.trluca.bsp

import akka.actor._
import akka.cluster.Member
import akka.pattern.ask
import it.unipd.trluca.bsp.aggregators.LocalAgentClock
import it.unipd.trluca.bsp.engine.{JobNode, PhaseTerminated, ExecutePhase}
import it.unipd.trluca.bsp.engine.aggregators.{ExecutePhaseLocal, LocalPhaseClock}

import scala.collection.SortedSet
import scala.concurrent.ExecutionContext.Implicits.global

case class CreateAgentConn(localAgents:Array[ActorRef], clusterMembers:SortedSet[Member], nr:Int)
case class PrintResultLocal(localAgents:Array[ActorRef])

class NodeRootActor extends JobNode with Actor with ActorLogging {

  override def receive = super.receive orElse localReceive

  def localReceive:Receive = {
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

    case PrintResult =>
      val orSender = sender()
      val wc = context.actorOf(Props[LocalAgentClock])
      val response = wc ? PrintResultLocal(localAgents)
      response map { Done =>
        orSender ! Done
      }

    case TakeDownCluster =>
      context.system.shutdown()
  }
}
