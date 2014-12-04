package it.unipd.trluca.bsp

import akka.actor.{Props, Actor}
import akka.pattern.ask
import scala.reflect.ClassTag

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Active(s:Boolean)

abstract  class Agent[S:ClassTag, T <:Agent[S, T]] extends Actor {
  case class Run(phase:Int)

  implicit val timeout = ConstStr.MAIN_TIMEOUT

  val messageReceiver = context.actorOf(Props[MessageReceiver[S]], "mr")

  def receive = {
    case ExecutePhaseLocal(localAgents, phase) =>
      val orSender = sender()
      val fInbox = (messageReceiver ? GetInbox).mapTo[Array[S]]
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
