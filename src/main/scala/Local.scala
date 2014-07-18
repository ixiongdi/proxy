package net.imiui.proxy

import akka.actor.ActorSystem
import akka.actor.Props

object Local {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Proxy")
    system.actorOf(Props(classOf[Client], "localhost", 3000), "Local")
  }
}
