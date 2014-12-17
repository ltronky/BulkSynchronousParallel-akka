package it.unipd.trluca.bsp.engine

import akka.actor.{ActorRef, Props, Actor, ActorLogging}
import akka.pattern.ask
import it.unipd.trluca.bsp.ConstStr
import it.unipd.trluca.bsp.engine.aggregators.{ExecutePhaseLocal, LocalPhaseClock}
import scala.concurrent.ExecutionContext.Implicits.global

trait JobNode extends Actor with ActorLogging {
  implicit val timeout = ConstStr.MAIN_TIMEOUT
  var localAgents:Array[ActorRef] = null

  def receive = {
    case ExecutePhase(phase) =>
      val orSender = sender()
      val wc = context.actorOf(Props[LocalPhaseClock])
      val response = (wc ? ExecutePhaseLocal(localAgents, phase)).mapTo[PhaseTerminated]
      response map { pt:PhaseTerminated =>
        orSender ! pt
      }
  }
}
