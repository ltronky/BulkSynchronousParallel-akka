package it.unipd.trluca.bsp.engine

import akka.actor.{ActorSelection, ActorLogging, Props, Actor}
import akka.pattern.ask
import it.unipd.trluca.bsp.ConstStr
import it.unipd.trluca.bsp.engine.aggregators.{StartAgents, InitStartAgentsAggregator, PhaseClock}
import scala.concurrent.ExecutionContext.Implicits.global


case object InitStartAgents
case class ExecutePhase(phase:Int)
case class PhaseTerminated(agentsAlive:Int)
case object JobTerminated

trait Job[S, T <: Agent[S, T]] extends Actor with ActorLogging {
    implicit val timeout = ConstStr.MAIN_TIMEOUT

    def startAgents():List[ActorSelection]

    def shouldRunAgain(phase:Int):Boolean

    def receive = {
        case InitStartAgents =>
            val act = context.actorOf(Props[InitStartAgentsAggregator])
            val response = act ? StartAgents(startAgents(), Message(0, act))
            response map { Done =>
                self ! ExecutePhase(0)
            }

        case ExecutePhase(phase) =>
            log.info(s"ExecutingPhase($phase)")
            val pc = context.actorOf(Props[PhaseClock])
            val response = (pc ? ExecutePhase(phase)).mapTo[PhaseTerminated]
            response map { ps: PhaseTerminated =>
                if (shouldRunAgain(phase + 1) && ps.agentsAlive > 0) {
                    self ! ExecutePhase(phase + 1)
                } else {
                    self ! JobTerminated
                }
            }
    }
}
