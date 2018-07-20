package fssi.interpreter

import java.nio.file.Paths

import fssi.ast.domain._
import fssi.ast.domain.types.Contract.UserContract
import fssi.ast.domain.types._
import fssi.interpreter.util.Once
import fssi.interpreter.util.trie.{Store, Trie}
import org.slf4j.LoggerFactory
import io.circe.syntax._
import io.circe.parser._
import fssi.interpreter.jsonCodec._

class ContractStoreHandler extends ContractStore.Handler[Stack] {
  private val contractTrie: Once[Trie] = Once.empty
  private val log = LoggerFactory.getLogger(getClass)

  private def _init(setting: Setting): Unit = {
    contractTrie := {
      val p = setting.workFileOfName("contract")
      Paths.get(p).toFile.mkdirs()
      Trie.empty(Store.levelDB(p))
    }
  }


  override def init(): Stack[Unit] = Stack {setting =>
    _init(setting)
  }

  override def saveContract(contract: Contract): Stack[Unit] = Stack {setting =>
    contract match {
      case x: UserContract =>
        val key = x.key
        val value = x.asJson.noSpaces
        contractTrie.foreach {trie =>
          trie.store.save(key.getBytes("utf-8"), value.getBytes("utf-8"))
          log.info(s"saved user contract of key: $key")
        }
      case _ =>
        log.debug("non-UserContract is fixed, no need to save")
    }
  }

  override def findContract(name: Contract.Name,
                            version: Contract.Version): Stack[Option[Contract]] = Stack {
    import Contract.inner._
    (name, version) match {
      case (TransferContract.name, TransferContract.version) => Some(TransferContract)
      case (PublishContract.name, PublishContract.version)   => Some(PublishContract)
      case (Contract.Name(n), Contract.Version(v))           =>
        val json = contractTrie.map {trie =>
          val key = n + "#" + v
          trie.store.load(key.getBytes("utf-8")).map(x => new String(x, "utf-8"))
        }.unsafe()

        json.flatMap {x =>
          (for {
            jso <- parse(x)
            con <- jso.as[UserContract]
          } yield con).toOption
        }

      case _ => None
    }
  }
}
object ContractStoreHandler {
  private val instance = new ContractStoreHandler
  trait Implicits {
    implicit val contractStoreHandler: ContractStoreHandler = instance
  }
  object implicits extends Implicits
}
