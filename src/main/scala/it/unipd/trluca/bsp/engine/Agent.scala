package it.unipd.trluca.bsp.engine

import akka.actor.{Actor, Props}
import akka.pattern.ask
import it.unipd.trluca.bsp._
import it.unipd.trluca.bsp.engine.aggregators.ExecutePhaseLocal

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag

case class Active(s:Boolean)

abstract  class Agent[S:ClassTag, T <:Agent[S, T]] extends Actor {

  implicit val timeout = ConstStr.MAIN_TIMEOUT

  val messageReceiver = context.actorOf(Props[MessageReceiver[S]], "mr")

  def receive = {
    case ExecutePhaseLocal(localAgents, phase) =>
      val orSender = sender()
      val fInbox = (messageReceiver ? GetInbox(phase)).mapTo[Array[S]]
      fInbox map { m =>
        if (m.isEmpty) {
          orSender ! PhaseTerminated(0)
        } else {
          run(m.toList, phase).mapTo[Active].map { a:Active =>
            orSender ! PhaseTerminated(if (a.s) 1 else 0)
          }
        }
      }
  }

  protected def run(m:List[S], phase:Int):Future[Any]

}
