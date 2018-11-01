package fssi
package base

trait Base58 {

  private val ALPHABET     = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray
  private val ENCODED_ZERO = ALPHABET(0)
  private val INDEX = {
    val tmp = collection.mutable.ListBuffer.fill[Int](128)(-1)
    ALPHABET
      .foldLeft((tmp, 0)) {
        case ((x, i), n) =>
          x(n.toInt) = i
          (x, i + 1)
      }
      ._1
  }

  private def divMod(number: Array[Byte],
                     firstDigit: Int,
                     base: Int,
                     divisor: Int): (Array[Byte], Byte) = {
    def loop(number: Vector[Byte], acc: (Vector[Byte], Int)): (Vector[Byte], Int) = {

      if (number.nonEmpty) {
        val digit     = number.head.toInt & 0xFF
        val remainder = acc._2.toInt
        val temp      = remainder * base + digit
        val ni        = (temp / divisor).toByte
        //println(s"digit=$digit,temp=$temp,remainder=$remainder,divisor=$divisor,ni=$ni")
        val accNext = (acc._1 :+ ni, temp % divisor)
        loop(number.drop(1), accNext)
      } else acc
    }

    val (n, r) = loop(number.drop(firstDigit).toVector, (Vector.empty, 0))

    (number.slice(0, firstDigit) ++ n.toArray[Byte], r.toByte)
  }

  def encode(x: Array[Byte]): String = {
    val zeros = x.takeWhile(_ == 0)
    //val nonZeros = x.dropWhile(_ == 0)

    def _encode(x: Array[Byte], inputStart: Int, acc: Vector[Char]): Vector[Char] =
      if (inputStart >= x.length) acc
      else {
        val (xNext, remainder) = divMod(x, inputStart, base = 256, divisor = 58)
        val accNext            = ALPHABET(remainder.toInt) +: acc
        val inputStartNext     = if (xNext(inputStart) == 0) inputStart + 1 else inputStart
        _encode(xNext, inputStartNext, accNext)
      }

    val encoded       = _encode(x, zeros.length, Vector.empty)
    val encodedNoZero = encoded.dropWhile(_ == ENCODED_ZERO)
    new String(zeros.map(_ => ENCODED_ZERO) ++ encodedNoZero.toArray[Char])
  }

  def decode(s: String): Option[Array[Byte]] = {
    val input58: Array[Int] = s.toCharArray.map { c =>
      if (c.toInt < 128) INDEX(c.toInt) else -1
    }

    if (input58.contains(-1)) None
    else {
      val bInput58 = input58.map(_.toByte)
      val zeros    = bInput58.takeWhile(_ == 0)

      def _decode(x: Array[Byte], inputStart: Int, acc: Vector[Byte]): Vector[Byte] =
        if (inputStart >= x.length) acc
        else {
          val (xNext, remainder) = divMod(x, inputStart, base = 58, divisor = 256)
          val accNext            = remainder +: acc
          val inputStartNext     = if (xNext(inputStart) == 0) inputStart + 1 else inputStart
          _decode(xNext, inputStartNext, accNext)
        }
      val decoded = _decode(bInput58, zeros.length, Vector.empty)
      Some(zeros ++ decoded.dropWhile(_ == 0).toArray[Byte])
    }

  }
}

object Base58 extends Base58
