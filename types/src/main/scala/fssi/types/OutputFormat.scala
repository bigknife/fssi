package fssi.types

/**
  * Created on 2018/8/14
  */
sealed trait OutputFormat

object OutputFormat {
  case object Jar    extends OutputFormat
  case object Hex    extends OutputFormat
  case object Base64 extends OutputFormat

  def apply(format: String): OutputFormat = format match {
    case j if Jar.toString.equalsIgnoreCase(j)    ⇒ Jar
    case h if Hex.toString.equalsIgnoreCase(h)    ⇒ Hex
    case b if Base64.toString.equalsIgnoreCase(b) ⇒ Base64
    case x                                        ⇒ throw new IllegalArgumentException(s"unsupported output format $x")
  }
}
