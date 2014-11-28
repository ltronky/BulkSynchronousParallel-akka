package it.unipd.trluca.bsp

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import scopt.OptionParser

case class Config(debug:Boolean = false,
                  clusterSize: Int = -1,
//                  arraySize: Int = 10,
//                  valueRange:Int = 10000,
                  nodes: Seq[String] = Seq())

object Main {
  def main(args: Array[String]): Unit = {

    val parser = new OptionParser[Config]("scopt") {
      head("Distributed-Array Sorting", "1.0")
      opt[Unit]('d', "debug") optional() action { (x, c) =>
        c.copy(debug = true)
      } text "Debug"
      opt[Int]('c', "cSize") action { (x, c) =>
        c.copy(clusterSize = x)
      } text "Initial Cluster Size"
//      opt[Int]('a', "arraySize") action { (x, c) =>
//        c.copy(arraySize = x)
//      } text "Distributed Array Size"
//      opt[Int]('r', "valueRange") optional() action { (x, c) =>
//        c.copy(valueRange = x)
//      } text "Values range"

      help("help") text "prints this usage text"

      arg[String]("<host:port>...") unbounded() action { (x, c) =>
        c.copy(nodes = c.nodes :+ x) } text "Nodes"
    }

    parser.parse(args, Config()) map { c =>

      c.nodes foreach { h=>
        val Array(hostname, port) = h.split(":")
        val conf = ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$hostname")
          .withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port"))
          .withFallback(
            if(c.debug)
              ConfigFactory.load("app_debug")
            else
              ConfigFactory.load()
          )
//        val sys = ActorSystem("ClusterSystem", conf)
//        sys.actorOf(Props[DistArrayNodeActor], "ablock")
//
//        if ((c.debug && port == "2551") || (!c.debug && c.arraySize != -1)) {
//          val ep = sys.actorOf(Props[EntryPoint], "ep")
//          ep ! SetDistArraySize(c)
//        }
      }
    } //getOrElse {}

  }
}
