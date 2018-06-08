package fssi.interpreter

import java.nio.file.Paths

import fssi.ast.domain._
import fssi.ast.domain.types.Contract.Parameter
import fssi.ast.domain.types.Contract.Parameter.{PArray, PBigDecimal, PString}
import fssi.ast.domain.types._
import fssi.contract.States
import fssi.interpreter.util.Once
import fssi.interpreter.util.trie.{Store, Trie}

class LedgerStoreHandler extends LedgerStore.Handler[Stack] {
  val accountStateTrie: Once[Trie] = Once.empty

  private def init(setting: Setting): Unit = {
    accountStateTrie := {
      val path = Paths.get(setting.workingDir, "states")
      path.toFile.mkdirs()
      Trie.empty(Store.levelDB(path.toString))
    }
  }

  override def loadStates(invoker: Account.ID,
                          contract: Contract,
                          parameter: Option[Parameter]): Stack[States] = Stack { setting =>
    init(setting)
    accountStateTrie.map { trie =>
      val accountIds: Vector[String] = contract match {
        case Contract.inner.TransferContract =>
          parameter
            .map {
              case PArray(Array(PString(to), PBigDecimal(_))) => Vector(invoker.value, to)
              case _                                          => Vector.empty
            }
            .getOrElse(Vector.empty)

        case Contract.inner.PublishContract => Vector(invoker.value)

        case x: Contract.UserContract =>
          //todo extract META-INF/account from the contract code
          Vector(invoker.value)
      }
    //val senderState = trie.store.load(transaction.sender.value.getBytes)
    }
    ???
  }
}

object LedgerStoreHandler {
  trait Implicits {
    implicit val ledgerStoreHandler: LedgerStoreHandler = new LedgerStoreHandler
  }

  object implicits extends Implicits
}
