package fssi
package trie

import org.scalatest._
import Trie._, Node._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._

class TrieSpec extends FunSuite {
  object implicits extends Bytes.Implicits with TrieCodecs with Bytes.Syntax
  import implicits._

  test("trie spec") {
    val k1 = Array(0, 1, 2, 3)
    val k2 = Array(0, 1, 2, 3)

    info(s"${Node.maxPrefix(k1, k2).map(_.toString).mkString(",")}")


    val t1 = Trie[Byte, String].put("hello,world".getBytes(), "Hello,world")
    val t2 = t1.put("hello".getBytes(), "GoodBoy")
    val t3 = t2.put("h".getBytes(), "Shit")

    info(s"$t1")
    info(s"$t2")
    info(s"$t3")
    info(s"${t3.asJson}")


    val node = Node(Array('h'), "hi")
    info(s"$node")
    info(s"${node.hexHash}")


    val node2 = Node(Array('h', 'e', 'l', 'l', 'o'), "world")
    info(s"$node2")

    val node3 = Node(Array('w', 'o', 'r', 'l', 'd'), "hello")
    info(s"$node3")



    val n1 = node.put(Array('h', 'e', 'l', 'l', 'o'), "world")
    info(s"$n1")
    info(s"${n1.get(Array('h'))}")
    info(s"${n1.get(Array('h', 'e', 'l', 'l', 'o'))}")

    val node4 = node.put(Array('h', 'e', 'l', 'l', 'o'), "world")
    info(s"$node4")

    val node5 = node4.put(Array('h', 'o', 'w'), "Heii")
    info(s"$node5")
    info(s"${node5.get(Array('h', 'o', 'w'))}")
    info(s"${node5.get(Array('h', 'e', 'l', 'l', 'o'))}")

    val node6 = node5.put(Array('w','h','a', 't'), "Fuck")
    info(s"$node6")
    info(s"${node6.get(Array('w','h','a', 't'))}")
    info(s"${node6.get(Array('h', 'o', 'w'))}")
    info(s"${node6.get(Array('h', 'e', 'l', 'l', 'o'))}")
    info(s"${node.hexHash}")
    info(s"${node2.hexHash}")
    info(s"${node3.hexHash}")
    info(s"${node4.hexHash}")
    info(s"${node5.hexHash}")
    info(s"${node6.hexHash}")
    info(s"${node6.asJson}")
  }
}
