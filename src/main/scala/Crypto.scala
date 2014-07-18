package net.imiui.proxy

import javax.crypto._
import javax.crypto.spec.SecretKeySpec

object Crypto {

  private final val secret: String = "http://imiui.net"

  private final val transformation: String = "AES"

  private final val raw = secret.getBytes

  private final val skeySpec = new SecretKeySpec(raw, "AES")

  private final val cipher = Cipher.getInstance(transformation)

  private def crypto(bytes: Array[Byte], mode: Int): Array[Byte] = {
    cipher.init(mode, skeySpec)
    cipher.doFinal(bytes)
  }

  def encrypt(bytes: Array[Byte]): Array[Byte] = {
    crypto(bytes, Cipher.ENCRYPT_MODE)
  }

  def decrypt(bytes: Array[Byte]): Array[Byte] = {
    crypto(bytes, Cipher.DECRYPT_MODE)
  }
}
