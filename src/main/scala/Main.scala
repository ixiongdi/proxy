// import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
// import akka.io.{ IO, Tcp }
// import akka.util.ByteString
// import java.net.InetSocketAddress
// import akka.event.Logging

// object Main {
//   def main(args: Array[String]): Unit = {
//     val system = ActorSystem("Proxy")
//     val local = system.actorOf(Props[Local], "Local")
//     val remote = system.actorOf(Props[Remote], "Remote")
//   }
// }

// class Local extends Actor {

//   val log = Logging(context.system, this)
 
//   import Tcp._
//   import context.system
 
//   IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 3000))
 
//   def receive = {
//     case b @ Bound(localAddress) =>
//       log.info(s"Bound: $b")
 
//     case CommandFailed(_: Bind) =>
//       log.info(s"Bind CommandFailed!")
//       context stop self
 
//     case c @ Connected(remote, local) =>
//       log.info("Connected!")
//       log.info("remote: " + remote.getHostName)
//       log.info("local: " + local.getHostName)
//       val handler = context.actorOf(Props[SimplisticHandler])
//       val connection = sender()
//       connection ! Register(handler)
//   }
 
// }

// class SimplisticHandler extends Actor {
//   val log = Logging(context.system, this)
//   import Tcp._
//   var rhost = "127.0.0.1"
//   var rport = 80
//   def receive = {
//     case Received(data) =>
     
//       log.info("Received Browser Request!")
//       // println(s"data: $data")
//       // println(data.utf8String)
//       if (data.length == 3) {
//         sender() ! Write(ByteString(5, 0))
//       } else if (data.length < 100) {
//         val atyp = data(3)
//         atyp match {
//           case 1 => {
//             // println(s"atype: $atyp")
//             val host = data.slice(4, data.length - 2).map {
//               b => if (b < 0) b + 256 else b
//             }
//             rhost = host(0).toString + "." + host(1).toString + "." + host(2).toString + "." + host(3).toString 
//             val port = data.slice(data.length - 2, data.length)
//             import io.netty.buffer.ByteBufUtil

//             rport = port.asByteBuffer.getShort
//             println(s"host: $rhost, port: $rport")
//             // rport = ByteBufUtil.swapInt(port.getBytes)
//             sender() ! Write(ByteString(5, 0, 0, 1, 8, 8, 8, 8, 0, 80))
//           } 
//           case 3 =>
//           case 4 => 
//         }
//       } else {
//             println(s"host: $rhost, port: $rport")
//         val sa = new InetSocketAddress(rhost, rport)
//         val c = context.system.actorOf(Props(classOf[Replies], sa, sender(), data))
//         // c ! data
//       }
//     case PeerClosed     => context stop self
//   }
// }

// case class Request(sa: InetSocketAddress, data: ByteString)
// case class Response(sa: InetSocketAddress, data: ByteString)

// class Replies(sa: InetSocketAddress, replay: ActorRef, data: ByteString) extends Actor {
//   import Tcp._
//   import context.system

//   val log = Logging(context.system, this)
 
//   IO(Tcp) ! Connect(new InetSocketAddress("localhost", 3001))
 
//   def receive = {
//     case CommandFailed(_: Connect) =>
//       context stop self
 
//     case c @ Connected(remote, local) =>
//       log.info(s"$remote, $local")
//       val connection = sender()
//       connection ! Register(self)
//       val sdata = ByteString(Crypto.encryptAES(sa.getHostName + ":::" + sa.getPort + ":::" + data.utf8String))
//       // println(s"$sdata")
//       connection ! Write(sdata)
//       context become {
//         case data: ByteString =>
//           log.info("1")
//           connection ! Write(data)
//         case CommandFailed(w: Write) =>
//           log.info("2")
//           // O/S buffer was full
//         case Received(data) =>
//           log.info("3")
//           log.info("Local Received")
//           // log.info(Crypto.decryptAES(data.utf8String))
//           // log.debug("Received")
//           replay ! Write(ByteString(Crypto.decryptAES(data.toByteBuffer.array)))
//           // replay ! Close
//         case "close" =>
//           log.info("4")
//           connection ! Close
//         case _: ConnectionClosed =>
//           log.info("5")
//           context stop self
//       }
//   }
// }

// class Remote extends Actor {
 
//   import Tcp._
//   import context.system
 
//   IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 3001))

//   println("remote start!")
 
//   def receive = {
//     case b @ Bound(localAddress) =>
//       println(s"remote: $b")
//       // do some logging or setup ...
 
//     case CommandFailed(_: Bind) => context stop self
 
//     case c @ Connected(remote, local) =>
//       println(s"Remote: $remote, $local")
//       val handler = context.actorOf(Props[RequestHandler])
//       val connection = sender()
//       connection ! Register(handler)
//   }
 
// }

// class RequestHandler extends Actor {
//   import Tcp._
//   def receive = {
//     case Received(data) =>
//       println(s"remote Request")
//       val ss = Crypto.decryptAES(data.utf8String).split(":::").toList
//       val sa = new InetSocketAddress(ss(0), ss(1).toInt)
//       // println(ss)
//       context.system.actorOf(Props(classOf[ResponseHandler], sa, sender(), ByteString(ss(2))))
//     case PeerClosed     => context stop self
//   }
// }

// class ResponseHandler(r: InetSocketAddress, replay: ActorRef, data: ByteString) extends Actor {
//   import Tcp._
//   import context.system

//   val log = Logging(context.system, this)
 
//   IO(Tcp) ! Connect(r)
 
//   def receive = {
//     case CommandFailed(_: Connect) =>
//       context stop self
 
//     case c @ Connected(remote, local) =>
//       log.info(s"$remote, $local")
//       val connection = sender()
//       connection ! Register(self)
//       println(data.utf8String)
//       connection ! Write(data)
//       context become {
//         case data: ByteString =>
//           log.info("1")
//           connection ! Write(data)
//         case CommandFailed(w: Write) =>
//           log.info("2")
//           // O/S buffer was full
//         case Received(data) =>
//           log.info("3")
//           // println(s"data: $data")
//           // log.debug("Received")
//           // val (h, p, d) = Codecs.decryptAES(data)
//           // println("~~~~~~~~~~~~~~~~~~~~~~~~~")
//           // println(data.decodeString("utf-8"))
//           // println("~~~~~~~~~~~~~~~~~~~~~~~~~")
//           val sdata = ByteString(Crypto.encryptAES(data.toByteBuffer.array))
//           replay ! Write(sdata)
//           // replay ! Close
//         case "close" =>
//           log.info("4")
//           connection ! Close
//         case _: ConnectionClosed =>
//           log.info("5")
//           context stop self
//       }
//   }
// }

// object Codecs {

//   /**
//    * Computes the SHA-1 digest for a byte array.
//    *
//    * @param bytes the data to hash
//    * @return the SHA-1 digest, encoded as a hex string
//    */
//   def sha1(bytes: Array[Byte]): String = {
//     import java.security.MessageDigest
//     val digest = MessageDigest.getInstance("SHA-1")
//     digest.reset()
//     digest.update(bytes)
//     digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
//   }

//   /**
//    * Computes the MD5 digest for a byte array.
//    *
//    * @param bytes the data to hash
//    * @return the MD5 digest, encoded as a hex string
//    */
//   def md5(bytes: Array[Byte]): String = {
//     import java.security.MessageDigest
//     val digest = MessageDigest.getInstance("MD5")
//     digest.reset()
//     digest.update(bytes)
//     digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
//   }

//   /**
//    * Compute the SHA-1 digest for a `String`.
//    *
//    * @param text the text to hash
//    * @return the SHA-1 digest, encoded as a hex string
//    */
//   def sha1(text: String): String = sha1(text.getBytes)

//   // --

//   private val hexChars =
//     Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

//   /**
//    * Converts a byte array into an array of characters that denotes a hexadecimal representation.
//    */
//   def toHex(array: Array[Byte]): Array[Char] = {
//     val result = new Array[Char](array.length * 2)
//     for (i <- 0 until array.length) {
//       val b = array(i) & 0xff
//       result(2 * i) = hexChars(b >> 4)
//       result(2 * i + 1) = hexChars(b & 0xf)
//     }
//     result
//   }

//   /**
//    * Converts a byte array into a `String` that denotes a hexadecimal representation.
//    */
//   def toHexString(array: Array[Byte]): String = {
//     new String(toHex(array))
//   }

//   /**
//    * Transform an hexadecimal String to a byte array.
//    */
//   def hexStringToByte(hexString: String): Array[Byte] = {
//     import org.apache.commons.codec.binary.Hex;
//     Hex.decodeHex(hexString.toCharArray());
//   }

// }

// /*
//  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
//  */
// import javax.crypto._
// import javax.crypto.spec.SecretKeySpec

// import java.security.SecureRandom
// import org.apache.commons.codec.binary.Hex

// object Crypto {

//   private def secret: String = "http://imiui.net"

//   private lazy val provider: Option[String] = None

//   private lazy val transformation: String = "AES_256/CFB/NoPadding"

//   private val random = new SecureRandom()

//   def sign(message: String, key: Array[Byte]): String = {
//     val mac = provider.map(p =>
//       Mac.getInstance("HmacSHA1", p)).getOrElse(Mac.getInstance("HmacSHA1"))
//     mac.init(new SecretKeySpec(key, "HmacSHA1"))
//     Codecs.toHexString(mac.doFinal(message.getBytes("utf-8")))
//   }

//   def sign(message: String): String = {
//     sign(message, secret.getBytes("utf-8"))
//   }

//   def signToken(token: String): String = {
//     val nonce = System.currentTimeMillis()
//     val joined = nonce + "-" + token
//     sign(joined) + "-" + joined
//   }

//   def extractSignedToken(token: String): Option[String] = {
//     token.split("-", 3) match {
//       case Array(signature, nonce, raw)
//         if constantTimeEquals(signature, sign(nonce + "-" + raw)) => Some(raw)
//       case _ => None
//     }
//   }

//   def generateToken = {
//     val bytes = new Array[Byte](12)
//     random.nextBytes(bytes)
//     new String(Hex.encodeHex(bytes))
//   }

//   def generateSignedToken = signToken(generateToken)

//   def compareSignedTokens(tokenA: String, tokenB: String) = {
//     (for {
//       rawA <- extractSignedToken(tokenA)
//       rawB <- extractSignedToken(tokenB)
//     } yield constantTimeEquals(rawA, rawB)).getOrElse(false)
//   }

//   def constantTimeEquals(a: String, b: String) = {
//     if (a.length != b.length) {
//       false
//     } else {
//       var equal = 0
//       for (i <- 0 until a.length) {
//         equal |= a(i) ^ b(i)
//       }
//       equal == 0
//     }
//   }

//   def encryptAES(bytes: Array[Byte]): Array[Byte] = {
//     val raw = secret.getBytes("utf-8")
//     val skeySpec = new SecretKeySpec(raw, "AES")
//     val cipher = provider.map(p =>
//       Cipher.getInstance(transformation, p)).getOrElse(Cipher.getInstance(transformation))
//     cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
//     cipher.doFinal(bytes)
//   }

//   def encryptAES(value: String): String = {
//     encryptAES(value, secret.substring(0, 16))
//   }

//   def encryptAES(value: String, privateKey: String): String = {
//     val raw = privateKey.getBytes("utf-8")
//     val skeySpec = new SecretKeySpec(raw, "AES")
//     val cipher = provider.map(p =>
//       Cipher.getInstance(transformation, p)).getOrElse(Cipher.getInstance(transformation))
//     cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
//     Codecs.toHexString(cipher.doFinal(value.getBytes("utf-8")))
//   }

//   def decryptAES(bytes: Array[Byte]): Array[Byte] = {
//     val raw = secret.getBytes("utf-8")
//     val skeySpec = new SecretKeySpec(raw, "AES")
//     val cipher = provider.map(p =>
//       Cipher.getInstance(transformation, p)).getOrElse(Cipher.getInstance(transformation))
//     cipher.init(Cipher.DECRYPT_MODE, skeySpec)
//     cipher.doFinal(bytes)
//   }

//   def decryptAES(value: String): String = {
//     decryptAES(value, secret.substring(0, 16))
//   }

//   def decryptAES(value: String, privateKey: String): String = {
//     val raw = privateKey.getBytes("utf-8")
//     val skeySpec = new SecretKeySpec(raw, "AES")
//     val cipher = provider.map(p =>
//       Cipher.getInstance(transformation, p)).getOrElse(Cipher.getInstance(transformation))
//     cipher.init(Cipher.DECRYPT_MODE, skeySpec)
//     new String(cipher.doFinal(Codecs.hexStringToByte(value)))
//   }

// }
