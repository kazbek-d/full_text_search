package common


class Adler {

  val MOD_ADLER = 65521
  var a = 1
  var b = 0

  def processAdler32sum(byte: Byte): Unit = {
    a = (byte + a) % MOD_ADLER
    b = (b + a) % MOD_ADLER
  }

  def getAdler32sum = (b << 16) + a

}
