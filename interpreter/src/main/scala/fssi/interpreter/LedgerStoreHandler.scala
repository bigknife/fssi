package fssi.interpreter

import java.nio.file.Paths

import fssi.ast.domain._
import fssi.ast.domain.exceptions.WorldStatesError
import fssi.ast.domain.types.Contract.Parameter
import fssi.ast.domain.types.Contract.Parameter.{PArray, PBigDecimal, PString}
import fssi.ast.domain.types._
import fssi.contract.States
import fssi.contract.AccountState
import fssi.interpreter.util.Once
import fssi.interpreter.util.trie.{Store, Trie}
import io.circe.parser._
import fssi.interpreter.jsonCodec._

import scala.collection.immutable

class LedgerStoreHandler extends LedgerStore.Handler[Stack] {
  val accountStateTrie: Once[Trie] = Once.empty

  private def _init(setting: Setting): Unit = {
    accountStateTrie := {
      val path = Paths.get(setting.workingDir, "states")
      path.toFile.mkdirs()
      Trie.empty(Store.levelDB(path.toString))
    }
  }


  override def init(): Stack[Unit] = Stack {setting =>
    _init(setting)
  }

  def clean(): Unit = {
    accountStateTrie.foreach(_.store.shutdown())
    accountStateTrie.reset()
  }

  override def loadStates(invoker: Account.ID,
                          contract: Contract,
                          parameter: Option[Parameter]): Stack[Either[WorldStatesError, States]] =
    Stack { setting =>
      //init(setting)
      accountStateTrie
        .map { trie =>
          // parameters of a transaction were resolved by ContractServiceHandler#resolveTransaction
          val accountIds: Vector[String] = contract match {
            case Contract.inner.TransferContract =>
              parameter
                .map {
                  case PArray(Array(PString(to), PBigDecimal(_))) =>
                    Vector(invoker.value, to) // to, amount
                  case _ => Vector.empty
                } getOrElse Vector.empty

            case Contract.inner.PublishContract =>
              parameter.map {
                case PArray(Array(PString(_), PString(_), PString(_), PString(_))) =>
                  Vector(invoker.value)
                case _ => Vector.empty
              } getOrElse Vector.empty

            case x: Contract.UserContract =>
              //extract META-INF/account from the contract code
              val rand   = java.util.UUID.randomUUID().toString
              val tmpDir = Paths.get(setting.contractTempDir, rand)
              tmpDir.toFile.mkdirs()
              val jarFile = Paths.get(tmpDir.toString, s"${x.name.value}.jar")
              better.files
                .File(jarFile)
                .writeByteArray(BytesValue.decodeBase64(x.code.base64).bytes)

              // then extract jarFile
              better.files.File(jarFile).unzipTo(better.files.File(tmpDir))
              val contractMetaFile = better.files.File(s"$tmpDir/META-INF/accounts")

              // Got the accounts
              val ret =  Vector(invoker.value) ++ contractMetaFile.lines.toVector

              better.files.File(tmpDir).delete()

              ret
          }

          val s: immutable.Seq[Either[WorldStatesError, AccountState]] = accountIds.map {
            storeKey =>
              trie.store
                .load(storeKey.getBytes)
                .map(x => new String(x))
                .map(parse)
                .map {
                  case Left(t) => Left(WorldStatesError(storeKey, Some(t)))
                  case Right(json) =>
                    json.as[AccountState] match {
                      case Left(t)             => Left(WorldStatesError(new String(storeKey), Some(t)))
                      case Right(accountState) => Right(accountState)
                    }
                }
                .getOrElse(Right(AccountState.emptyFor(storeKey)))
          }

          // exists failed
          if (s.exists(_.isLeft)) Left(s.filter(_.isLeft).map(_.left.get).head)
          else Right(States(s.map(_.right.get)))
        }
        .unsafe()
    }
}

object LedgerStoreHandler {
  private val instance = new LedgerStoreHandler
  trait Implicits {
    implicit val ledgerStoreHandler: LedgerStoreHandler = instance
  }

  object implicits extends Implicits
}
