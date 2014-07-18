package net.imiui.proxy

import akka.actor.ActorSystem
import akka.actor.Props

object Remote {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Proxy")
    system.actorOf(Props[Server], "Remote")
  }
}
