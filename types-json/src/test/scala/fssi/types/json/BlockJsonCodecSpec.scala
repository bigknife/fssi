package fssi
package types
package json
import fssi.types.base.{Hash, Signature, Timestamp, WorldState}
import fssi.types.biz._
import fssi.types.base.WorldState
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import json.implicits._
import org.scalatest.FunSuite
import io.circe.parser._

class BlockJsonCodecSpec extends FunSuite {

  test("test block json coder") {
    val previousBlockState    = WorldState("previousBlockState".getBytes())
    val previousTokenState    = WorldState("previousTokenState".getBytes())
    val previousContractState = WorldState("previousContractState".getBytes())
    val previousDataState     = WorldState("previousDataState".getBytes())
    val previousReceiptState  = WorldState("previousReceiptState".getBytes())

    val currentBlockState    = WorldState("currentBlockState".getBytes())
    val currentTokenState    = WorldState("currentTokenState".getBytes())
    val currentContractState = WorldState("currentContractState".getBytes())
    val currentDataState     = WorldState("currentDataState".getBytes())
    val currentReceiptState  = WorldState("currentReceiptState".getBytes())

    val previousWorldStates = WorldState(
      blockState = previousBlockState,
      tokenState = previousTokenState,
      contractState = previousContractState,
      dataState = previousDataState,
      receiptState = previousReceiptState
    )
    val currentWorldStates = WorldState(
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
