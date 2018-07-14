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
import fssi.interpreter.util.trie.{Store, StoreKey, StoreValue, Trie}
import io.circe.parser._
import io.circe.syntax._
import fssi.interpreter.jsonCodec._
import org.slf4j.LoggerFactory

import scala.collection.immutable

class LedgerStoreHandler extends LedgerStore.Handler[Stack] {
  private val logger = LoggerFactory.getLogger(getClass)

  val accountStateTrie: Once[Trie] = Once.empty
  val timeCapsuleTrie: Once[Trie]  = Once.empty

  private val TimeCapsuleHeightKey: StoreKey = Array(0.toByte, 0.toByte, 0.toByte, 0.toByte)

  private def _init(setting: Setting): Unit = {
    accountStateTrie := {
      val path = Paths.get(setting.workingDir, "states")
      path.toFile.mkdirs()
      Trie.empty(Store.levelDB(path.toString))
    }

    timeCapsuleTrie := {
      val path = Paths.get(setting.workingDir, "timecapsule")
      path.toFile.mkdirs()
      Trie.empty(Store.levelDB(path.toString))
    }
  }

  override def init(): Stack[Unit] = Stack { setting =>
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
              val ret = Vector(invoker.value) ++ contractMetaFile.lines.toVector

              better.files.File(tmpDir).delete()

              ret
          }

          val s: immutable.Seq[Either[WorldStatesError, AccountState]] = accountIds.map {
            storeKey =>
              trie.store
                .load(storeKey.getBytes)
                .map(x => new String(x, "utf-8"))
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

  override def currentHeight(): Stack[BigInt] = Stack {
    // a fix path to save current height
    timeCapsuleTrie
      .map { trie =>
        trie.store.load(TimeCapsuleHeightKey).map(x => BigInt(new String(x, "utf-8"))).getOrElse(BigInt(0))
      }
      .unsafe()
  }

  override def saveStates(states: Map[String, AccountState]): Stack[Unit] = Stack {
    accountStateTrie.foreach { trie =>
      states.foreach {
        case (key, state) =>
          val storeKey: StoreKey     = key.getBytes("utf-8")
          val storeValue: StoreValue = state.asJson.noSpaces.getBytes("utf-8")
          trie.store.save(storeKey, storeValue)
      }
    }
  }

  override def updateHeight(height: BigInt): Stack[Unit] = Stack {
    timeCapsuleTrie.foreach {trie =>
      trie.store.save(TimeCapsuleHeightKey, height.toString().getBytes("utf-8"))
    }
  }

  override def timeCapsuleOf(height: BigInt): Stack[TimeCapsule] = Stack {
    timeCapsuleTrie.map {trie =>
      trie.store.load(height.toString().getBytes) match {
        case None => throw new RuntimeException(s"no timecapsule found for $height")
        case Some(value) =>
          parse(new String(value, "utf-8")) match {
            case Left(t) => throw t
            case Right(json) =>
              json.as[TimeCapsule] match {
                case Left(t) => throw t
                case Right(timeCapsule) => timeCapsule
              }
          }
      }
    }.unsafe()
  }


  override def findTimeCapsuleAt(height: BigInt): Stack[Option[TimeCapsule]] = Stack {
    timeCapsuleTrie.map {trie =>
      trie.store.load(height.toString().getBytes).map {value =>
          parse(new String(value, "utf-8")) match {
            case Left(t) =>
              logger.warn("parse time capsule to json failed", t)
              None
            case Right(json) =>
              json.as[TimeCapsule] match {
                case Left(t) =>
                  logger.warn("parse time capsule json to TimeCapsule failed", t)
                  None
                case Right(timeCapsule) => Some(timeCapsule)
              }
          }
      }
    }.unsafe().flatten
  }

  override def saveTimeCapsule(timeCapsule: TimeCapsule): Stack[Unit] = Stack {
    timeCapsuleTrie.foreach {trie =>
      val storeKey: StoreKey = timeCapsule.height.toString.getBytes("utf-8")
      val storeValue: StoreValue = timeCapsule.asJson.noSpaces.getBytes("utf-8")
      trie.store.save(storeKey, storeValue)
    }
  }
}

object LedgerStoreHandler {
  private val instance = new LedgerStoreHandler
  trait Implicits {
    implicit val ledgerStoreHandler: LedgerStoreHandler = instance
  }

  object implicits extends Implicits
}
