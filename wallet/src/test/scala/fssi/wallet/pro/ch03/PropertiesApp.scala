package fssi.wallet.pro.ch03

import scalafx.beans.property.StringProperty

object PropertiesApp extends App {
  val prop1 = new StringProperty("")
  val prop2 = new StringProperty("")

  prop2 <==> prop1

  prop1() = "hello"

  println(prop2())

  prop2() = "world"
  println(prop1())
}
