package fssi
package types
package json
import fssi.types.base.{Hash, Signature, Timestamp, HashState}
import fssi.types.biz._
import fssi.types.biz.Block.WorldStates
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import json.implicits._
import org.scalatest.FunSuite
import io.circe.parser._

class BlockJsonCodecSpec extends FunSuite {

  test("test block json coder") {
    val previousBlockState    = HashState("previousBlockState".getBytes())
    val previousTokenState    = HashState("previousTokenState".getBytes())
    val previousContractState = HashState("previousContractState".getBytes())
    val previousDataState     = HashState("previousDataState".getBytes())
    val previousReceiptState  = HashState("previousReceiptState".getBytes())

    val currentBlockState    = HashState("currentBlockState".getBytes())
    val currentTokenState    = HashState("currentTokenState".getBytes())
    val currentContractState = HashState("currentContractState".getBytes())
    val currentDataState     = HashState("currentDataState".getBytes())
    val currentReceiptState  = HashState("currentReceiptState".getBytes())

    val previousWorldStates = WorldStates(
      blockState = previousBlockState,
      tokenState = previousTokenState,
      contractState = previousContractState,
      dataState = previousDataState,
      receiptState = previousReceiptState
    )
    val currentWorldStates = WorldStates(
      blockState = currentBlockState,
      tokenState = currentTokenState,
      contractState = currentContractState,
      dataState = currentDataState,
      receiptState = currentReceiptState
    )
    val height      = BigInt(10)
    val chainId     = "block chain id"
    val currentTime = System.currentTimeMillis()
    val timestamp   = Timestamp(currentTime)

    val head = Block.Head(previousStates = previousWorldStates,
                          currentStates = currentWorldStates,
                          height = height,
                          chainId = chainId,
                          timestamp = timestamp)

    val tId       = Transaction.ID("transactionId".getBytes())
    val tPayer    = Account.ID("transactionPayer".getBytes())
    val tPayee    = Account.ID("transactionPayee".getBytes())
    val tToken    = Token(amount = BigInt(100), tokenUnit = Token.Unit.Sweet)
    val signature = Signature("transactionSignature".getBytes())
    val transaction = Transaction.Transfer(id = tId,
                                           payer = tPayer,
                                           payee = tPayee,
                                           token = tToken,
                                           signature = signature,
                                           timestamp = currentTime)
    val transactions       = TransactionSet(transaction)
    val stackTraceElement  = new StackTraceElement("fssi.contract.banana", "register", "price", 50)
    val stackTraceElements = Vector(stackTraceElement)
    val log                = Receipt.Log("INFO", "255")
    val logs               = Vector(log)
    val receipt = Receipt(transactionId = tId,
                          success = true,
                          exception = Option(stackTraceElements),
                          logs = logs,
                          costs = 1000)
    val receipts = Set(receipt)
    val hash     = Hash("transactionHash".getBytes())
    val block    = Block(head = head, transactions = transactions, receipts = receipts, hash = hash)

    val jsonString = block.asJson.spaces2
    info(jsonString)

    val r = for {
      json <- parse(jsonString)
      res  <- json.as[Block]
    } yield res

    assert(r.isRight)
  }
}
