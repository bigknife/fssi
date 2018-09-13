package fssi
package interpreter
package scp

import types._, implicits._
import bigknife.scalap.ast.types._

/** Block Value
  * in order to sperate bytes-calculating from BlockValue types, we
  * set a bytes argument, which will be calcluted outside
  * @param bytes used to verify this block is legal or not.
  */
case class BlockValue(block: Block, bytes: Array[Byte] = Array.emptyByteArray) extends Value {
  def compare(that: Value): Int = that match {
    case BlockValue(thatBlock, _) =>
      if (block.height > thatBlock.height) 1
      else if (block.height < thatBlock.height) -1
      else {
        if (block.transactions.isEmpty && thatBlock.transactions.isEmpty) 0
        else if (block.transactions.isEmpty) -1
        else if (thatBlock.transactions.isEmpty) 1
        else {
          val thisTransaction = block.transactions.max
          val thatTransaction = thatBlock.transactions.max
          val x = thisTransaction.timestamp - thatTransaction.timestamp
          if (x > 0) 1 else if (x < 0) -1 else 0
        }
      }
    case _ => -1
  }
}
