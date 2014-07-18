package net.imiui.proxy

import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import akka.io.{ IO, Tcp, Udp }
import akka.util.ByteString
import akka.util.ByteStringBuilder
import java.net.InetSocketAddress

import io.netty.handler.codec.socks.SocksAddressType
import io.netty.handler.codec.socks.SocksAddressType._

class SocksTcpHelloRequest(ver: Byte, method: Byte, methods: ByteString) {

  def apply(bs: ByteString) = {
    val bi = bs.iterator
    val ver = bi.getByte
    val method = bi.getByte
    val methods = bi.toByteString 
  }

  def toByteString = {
    new ByteStringBuilder()
      .putByte(ver)
      .putByte(method)
      .append(methods)
      .result()
  }
}

class SocksTcpHelloResponse(ver: Byte, method: Byte) {

  def toByteString = {
    new ByteStringBuilder()
      .putByte(ver)
      .putByte(method)
      .result()
  }
}

class SocksTcpRequestHeader(ver: Byte, cmd: Byte, rsv: Byte, atyp: Byte, addr: ByteString, port: Short) {

  implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN

  def toShadowsocksTcp(data: ByteString) = {
    new ShadowsocksTcp(atyp, addr, port, data)
  }

  def toByteString = {
    new ByteStringBuilder()
      .putByte(ver)
      .putByte(cmd)
      .putByte(rsv)
      .putByte(atyp)
      .append(addr)
      .putShort(port)
      .result()
  }
}

class SocksTcpRequestHeaderReply(ver: Byte, cmd: Byte, rsv: Byte, atyp: Byte, addr: ByteString, port: Short) {

  implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN

  def toByteString = {
    new ByteStringBuilder()
      .putByte(ver)
      .putByte(cmd)
      .putByte(rsv)
      .putByte(atyp)
      .append(addr)
      .putShort(port)
      .result()
  }
}

class SocksTcpRequestBody(data: ByteString) {}

class SocksTcpResponse(data: ByteString) {}

case class ShadowsocksTcp(atyp: Byte, addr: ByteString, port: Short, data: ByteString) {

  implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN

  val ssAtyp = atyp

  val ssAddr = addr

  val ssPort = port

  val ssData = data

  def getAddr = {
    SocksAddressType.valueOf(atyp) match {
      case IPv4 =>
        SocksCommonUtils.intToIp(addr.iterator.getInt)
      case DOMAIN =>
        addr.drop(1).decodeString("US-ASCII")
      case IPv6 =>
        SocksCommonUtils.ipv6toStr(addr.drop(16).toArray)
      case UNKNOWN => throw new NullPointerException("responseType")
    }
  }

  def getPort = port.toInt

  def toByteString = {
    new ByteStringBuilder()
      .putByte(atyp)
      .append(addr)
      .putShort(port)
      .append(data)
      .result()
  }
}

class ShadowsocksTcpData(data: ByteString) {}

object Utils {

  implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN

  def parseToSocksTcpRequestHeader(bs: ByteString) = {

    val bi = bs.iterator

    val ver = bi.getByte
    val cmd = bi.getByte
    val rsv = bi.getByte
    val atyp = bi.getByte
    SocksAddressType.valueOf(atyp) match {
      case IPv4 =>
        val host = new ByteStringBuilder().putInt(bi.getInt).result
        val port = bi.getShort
        new SocksTcpRequestHeader(ver, cmd, rsv, atyp, host, port)
      case DOMAIN =>
        val xs = bi.toArray
        val host = ByteString(xs.slice(0, xs.head + 1))
        val port = ByteString(xs.slice(xs.head + 1, xs.head + 3)).iterator.getShort
        new SocksTcpRequestHeader(ver, cmd, rsv, atyp, host, port)
      case IPv6 =>
        val xs = bi.toArray
        val host = ByteString(xs.slice(0, 16))
        val port = ByteString(xs.slice(16, 18)).iterator.getShort
        new SocksTcpRequestHeader(ver, cmd, rsv, atyp, host, port)
      case UNKNOWN => throw new NullPointerException("responseType")
    }
  }

  def parseShadowsocksTcp(bs: ByteString) = {
    val bi = bs.iterator
    val atyp = bi.getByte
    SocksAddressType.valueOf(atyp) match {
      case IPv4 =>
        val host = new ByteStringBuilder().putInt(bi.getInt).result
        val port = bi.getShort
        new ShadowsocksTcp(atyp, host, port, bi.toByteString)
      case DOMAIN =>
        val xs = bi.toArray
        val host = ByteString(xs.slice(0, xs.head + 1))
        val port = ByteString(xs.slice(xs.head + 1, xs.head + 3)).iterator.getShort
        val data = ByteString(xs.drop(xs.head + 3))
        new ShadowsocksTcp(atyp, host, port, data)
      case IPv6 =>
        val xs = bi.toArray
        val host = ByteString(xs.slice(0, 16))
        val port = ByteString(xs.slice(16, 18)).iterator.getShort
        new ShadowsocksTcp(atyp, host, port, bi.toByteString)
      case UNKNOWN => throw new NullPointerException("responseType")
    }
  }
}