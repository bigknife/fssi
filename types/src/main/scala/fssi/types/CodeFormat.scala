package fssi.types

/**
  * Created on 2018/8/14
  */
sealed trait CodeFormat

object CodeFormat {
  case object Jar    extends CodeFormat
  case object Hex    extends CodeFormat
  case object Base64 extends CodeFormat

  def apply(format: String): CodeFormat = format match {
    case j if Jar.toString.equalsIgnoreCase(j)    ⇒ Jar
    case h if Hex.toString.equalsIgnoreCase(h)    ⇒ Hex
    case b if Base64.toString.equalsIgnoreCase(b) ⇒ Base64
    case x                                        ⇒ throw new IllegalArgumentException(s"unsupported output format $x")
  }
}
