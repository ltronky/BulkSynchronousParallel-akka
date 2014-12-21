package it.unipd.trluca.bsp

import akka.actor.{Actor, ActorLogging, Address}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

case class SetInitClusterSize(dim:Int)

class MemberListener extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  var initCSize = 0

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent])
    cluster.subscribe(self, classOf[LeaderChanged])
  }

  override def postStop(): Unit =
    cluster unsubscribe self

  var nodes = Set.empty[Address]

  def receive = {
    case SetInitClusterSize(dim) => initCSize = dim

    case state: CurrentClusterState =>
      log.info("Current members: {}", state.members.mkString(", "))
    case MemberUp(member) =>
      nodes += member.address
      log.info("Member is Up: {}. {} nodes in cluster", member.address, nodes.size)
      if (nodes.size == initCSize)
        context.actorSelection("/user/ep") ! StartExecution

    case MemberRemoved(member, _) =>
      nodes -= member.address
      log.info("Member is Removed: {}. {} nodes cluster", member.address, nodes.size)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)

    case LeaderChanged(address) =>
      log.info(s"leader changed: $address")

    //case _: MemberEvent => // ignore
    case m:Any => log.info("MessageLost:" + m)// ignore
  }

}
