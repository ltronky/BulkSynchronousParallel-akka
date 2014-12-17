package it.unipd.trluca.bsp

import akka.actor._
import akka.cluster.{Member, Cluster}
import akka.pattern.ask
import akka.util.Timeout
import it.unipd.trluca.bsp.aggregators.WorldClock
import it.unipd.trluca.bsp.engine.{InitStartAgents, JobTerminated, Message, Job}
import it.unipd.trluca.bsp.engine.aggregators.PhaseClock

import scala.collection.SortedSet
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global


object ConstStr {
  final val NODE_ACT_NAME = "/user/ablock"
  final val MAIN_TIMEOUT = Timeout(10.seconds) //TODO controllare non sia troppo breve per l'esecuzione
}

trait EngineStep
case class InitNode(size:Int) extends EngineStep
case class CreateConnections(clusterSize:SortedSet[Member], nr:Int) extends EngineStep
case object PrintResult extends EngineStep

case object TakeDownCluster

case object Done

case object StartExecution
case object InitAgentsConnections
case class SetInitialSize(c:Config)


class EntryPoint extends Job[Any, V] with Actor with ActorLogging {
  def actorRefFactory = context

  var agentsInNode = 0
  var clusterSize = 0

  override def receive = super.receive orElse localReceive

  def localReceive:Receive = {
    case SetInitialSize(config) =>
      agentsInNode = config.agentsInNode
      clusterSize = config.clusterSize
      val mL = context.actorOf(Props[MemberListener], "memberListener")
      mL ! SetInitClusterSize(config.clusterSize)

    case StartExecution =>
      val wc = context.actorOf(Props[WorldClock])
      val response = wc ? InitNode(agentsInNode)
      response map { Done =>
        self ! InitAgentsConnections
      }

    case InitAgentsConnections =>
      val wc = context.actorOf(Props[WorldClock])
      val response = wc ? CreateConnections(Cluster(context.system).state.members, 3) //TODO numero massimo di connesioni DA un nodo
      response map { Done =>
        self ! InitStartAgents
      }

    case JobTerminated =>
      log.info("JobTerminated")
      val wc = context.actorOf(Props[WorldClock])
      val response = wc ? PrintResult
      response map { Done =>
        log.info("PrintDone")
        takeDown()
      }
  }

  def takeDown() = {
    Cluster(context.system).state.members foreach { m=>
      context.actorSelection(m.address + ConstStr.NODE_ACT_NAME) ! TakeDownCluster
    }
  }

  override def shouldRunAgain(phase:Int) = true

  override def startAgents():List[ActorSelection] = {
    Array(context.actorSelection(Cluster(context.system).selfAddress + ConstStr.NODE_ACT_NAME + "/ag0/mr")).toList
  }
}
