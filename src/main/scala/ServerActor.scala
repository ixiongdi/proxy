package net.imiui.proxy

import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import akka.io.{ IO, Tcp, Udp }
import akka.util.ByteString
import akka.util.ByteStringBuilder
import java.net.InetSocketAddress


class Server extends Actor {

  val log = Logging(context.system, this)
 
  import Tcp._
  import context.system
 
  IO(Tcp) ! Bind(self, new InetSocketAddress("128.199.192.47", 3001))
 
  def receive = {
    case b @ Bound(localAddress) =>
      log.info(s"Bound: $b")
      // do some logging or setup ...
 
    case CommandFailed(_: Bind) =>
      log.info("CommandFailed")
      context stop self
 
    case c @ Connected(remote, local) =>
      log.info(s"Connected: $c")
      val handler = context.actorOf(Props[RequestHandler])
      val connection = sender()
      connection ! Register(handler)
  }
 
}

class RequestHandler extends Actor {

  val log = Logging(context.system, this)

  import Tcp._
  def receive = {
    case Received(data) =>
      val res = Utils.parseShadowsocksTcp(ByteString(Crypto.decrypt(data.toArray)))
      context.system.actorOf(Props(classOf[ResponseHandler], sender(), res))
    case PeerClosed     => context stop self
  }
}

class ResponseHandler(replay: ActorRef, req: ShadowsocksTcp) extends Actor {
  import Tcp._
  import context.system

  val log = Logging(context.system, this)
 
  IO(Tcp) ! Connect(new InetSocketAddress(req.getAddr, req.getPort))
 
  def receive = {
    case CommandFailed(_: Connect) =>
      context stop self
 
    case c @ Connected(remote, local) =>
      log.info(s"Connected: $c")
      val connection = sender()
      connection ! Register(self)
      connection ! Write(req.ssData)
      context become {
        case data: ByteString =>
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          log.info("CommandFailed")
        case Received(data) =>
          val res = new ShadowsocksTcp(req.ssAtyp, req.ssAddr, req.ssPort, data)
          replay ! Write(ByteString(Crypto.encrypt(res.toByteString.toArray)))
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          context stop self
      }
  }
}