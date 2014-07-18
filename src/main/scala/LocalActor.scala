package net.imiui.proxy

import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import akka.io.{ IO, Tcp, Udp }
import akka.util.ByteString
import akka.util.ByteStringBuilder
import java.net.InetSocketAddress

class Client(addr: String, port: Int) extends Actor {

  val log = Logging(context.system, this)
 
  import Tcp._
  import context.system
 
  IO(Tcp) ! Bind(self, new InetSocketAddress(addr, port))

  def receive = {
    case b @ Bound(localAddress) =>
      log.info(s"Bound: $b")
 
    case CommandFailed(b: Bind) =>
      log.info(s"CommandFailed: $b")
      context stop self
 
    case c @ Connected(remote, local) =>
      log.info("Connected")
      log.info(s"remote: $remote")
      log.info(s"local: $local")
      val handler = context.actorOf(Props[SocksHandler])
      val connection = sender()
      connection ! Register(handler)
  }
}

// # local:
// # stage 0 init
// # stage 1 hello received, hello sent
// # stage 2 UDP assoc
// # stage 3 DNS
// # stage 4 addr received, reply sent
// # stage 5 remote connected

// # remote:
// # stage 0 init
// # stage 3 DNS
// # stage 4 addr received, reply sent
// # stage 5 remote connected

class SocksHandler extends Actor {

  val log = Logging(context.system, this)

  import Tcp._
  import context.system

  var stage = 1
  
  var strh: SocksTcpRequestHeader = _

  def receive = {
    case Received(data) =>
      stage match {
        case 0 =>
          log.info("stage 0")
        case 1 =>
          log.info("stage 1")
          if (data == ByteString(5, 1, 0)) {
            stage = 4
            sender() ! Write(ByteString(5, 0))
          }
        case 2 =>
          log.info("stage 2")
        case 3 =>
          log.info("stage 3")
        case 4 =>
          log.info("stage 4")
          strh = Utils.parseToSocksTcpRequestHeader(data)
          stage = 5
          sender() ! Write(ByteString(5, 0, 0, 1, 8, 8, 8, 8, 0, 80))
        case 5 =>
          log.info("stage 5")
          context.system.actorOf(Props(classOf[Replay], strh.toShadowsocksTcp(data), sender()))
      }
    case PeerClosed => context stop self
  }
}

class Replay(str: ShadowsocksTcp, replay: ActorRef) extends Actor {

  val log = Logging(context.system, this)

  import Tcp._
  import context.system
 
  IO(Tcp) ! Connect(new InetSocketAddress("128.199.192.47", 3001))

 
  def receive = {
    case CommandFailed(_: Connect) =>
      context stop self
 
    case c @ Connected(remote, local) =>
      log.info(s"$c")
      val connection = sender()
      connection ! Register(self)
      connection ! Write(ByteString(Crypto.encrypt(str.toByteString.toArray)))
      context become {
        case data: ByteString =>
          log.info(s"$data")
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          log.info("CommandFailed")
        case Received(data) =>
          val sstr = Utils.parseShadowsocksTcp(ByteString(Crypto.decrypt(data.toArray)))
          replay ! Write(sstr.ssData)
        case "close" =>
          log.info("connection close")
          connection ! Close
        case _: ConnectionClosed =>
          log.info("connection closed")
          context stop self
      }
  }
}
