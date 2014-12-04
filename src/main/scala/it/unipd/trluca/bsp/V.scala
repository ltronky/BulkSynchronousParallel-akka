package it.unipd.trluca.bsp

import akka.actor.{ActorLogging, ActorSelection, Props}
import akka.cluster.Cluster
import akka.pattern.ask
import it.unipd.trluca.bsp.aggregators.{DispatchMessage, MessageDispatcher}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

object V {
  def props(id: Int): Props = Props(classOf[V], id)
}

class V(id:Int) extends Agent[Any, V] with ActorLogging {

  val edges = ArrayBuffer[ActorSelection]()
  var parent:Any = null

  override def receive = super.receive orElse localReceive
  def localReceive: Receive = {
    case CreateAgentConn(sameNodeAgents, clusterMembers, nrOfConn) =>
      val cMemb = clusterMembers.toArray
      val smaSize = sameNodeAgents.size
      val random = Random
      for (i <- 0 to nrOfConn-1) {
        val item = random.nextInt(smaSize)
        val randomNodeAddress = cMemb(random.nextInt(cMemb.size)).address
        val as = context.actorSelection(randomNodeAddress.toString + ConstStr.NODE_ACT_NAME + s"/ag$item/mr")
        if (!(edges contains as) && !(Cluster(context.system).selfAddress == randomNodeAddress && id == item)) {
          edges += as
        }
      }
      log.info("CE " + toString)
      sender() ! Done

    case PrintResult =>
      log.info("CE " + toString)
      sender() ! Done

    case _=>
  }


  override def run(m:List[Any], phase:Int): Future[Any] = {
    if (parent == null) parent = m(0)
    else if (phase > 0)
      return Future(Active(s = false))

    //log.info("VRun Agent->" + id + " parent->" + parent + " phase=" + phase)
    val mDispatcher = context.actorOf(Props[MessageDispatcher])
    mDispatcher ? DispatchMessage(edges, m)
  }

  override def toString = "V(" + id + " at " + Cluster(context.system).selfAddress +
    ") [Parent " + parent + " withEdges " + edges + "]"
}
