package it.unipd.trluca.bsp

import akka.actor._
import akka.cluster.{Member, Cluster}
import akka.pattern.ask
import akka.util.Timeout
import it.unipd.trluca.bsp.aggregators.{PhaseClock, WorldClock}

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
case object InitStartAgents
case class ExecutePhase(phase:Int)
case class SetInitialSize(c:Config)
case object JobTerminated
case class PhaseTerminated(agentsAlive:Int)

class EntryPoint extends Actor with ActorLogging {
  def actorRefFactory = context
  implicit val timeout = ConstStr.MAIN_TIMEOUT

  var agentsInNode = 0
  var clusterSize = 0

  def receive = {
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

    case InitStartAgents =>
      val act = context.actorSelection(Cluster(context.system).selfAddress + ConstStr.NODE_ACT_NAME + "/ag0/mr")
      val response = act ? Message(0, act)
      response map { ResReceived =>
        self ! ExecutePhase(0)
      }

    case ExecutePhase(phase) =>
      log.info(s"ExecutingPhase($phase)")
      val wc = context.actorOf(Props[PhaseClock])
      val response = (wc ? ExecutePhase(phase)).mapTo[PhaseTerminated]
      response map { ps:PhaseTerminated =>
        if (shouldRunAgain(phase+1) && ps.agentsAlive > 0) {
          self ! ExecutePhase(phase+1)
        } else {
          self ! JobTerminated
        }
      }


    case JobTerminated => log.info("JobTerminated")
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

  def shouldRunAgain(phase:Int) = true //TODO estrarre Engine e Job
}
