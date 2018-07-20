package fssi.interpreter

import java.nio.file._

import fssi.interpreter.util._
import fssi.ast.domain._
import fssi.ast.domain.exceptions._
import fssi.ast.domain.types._
import fssi.sandbox.{Runner, compiler}
import _root_.java.util.zip.{ZipEntry, ZipOutputStream}
import java.io._
import java.nio.file.attribute.BasicFileAttributes

import fssi.ast.domain.types.Contract.Parameter._
import fssi.contract.{AccountState, States}

import scala.annotation.tailrec
import fssi.interpreter.jsonCodec._
import io.circe.Decoder.Result
import io.circe.DecodingFailure
import io.circe.syntax._
import io.circe.parser._
import org.slf4j.{Logger, LoggerFactory}

class ContractServiceHandler extends ContractService.Handler[Stack] {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def createContractWithoutHash(owner: Account.ID,
                                         name: String,
                                         version: String,
                                         code: String): Stack[Contract.UserContract] = Stack {
    Contract.UserContract(
      owner,
      Contract.Name(name),
      Contract.Version(version),
      Contract.Code(code)
    )
  }

  override def createParameterFromString(
      params: String): Stack[Either[IllegalContractParams, Contract.Parameter]] = Stack {
    import io.circe.parser._
    import Contract.Parameter
    import Contract.Parameter._

    def parseParams(): Either[IllegalContractParams, Parameter] = {
      parse(params) match {
        case Left(t)                        => Left(IllegalContractParams(Some(t.getMessage())))
        case Right(value) if value.isString => Right(PString(value.asString.get))
        case Right(value) if value.isNumber =>
          Right(PBigDecimal(value.asNumber.flatMap(_.toBigDecimal).map(_.bigDecimal).get))
        case Right(value) if value.isBoolean => Right(PBool(value.asBoolean.get))
        case Right(values) if values.isArray =>
          val initData: Either[IllegalContractParams, PArray] = Right(PArray.Empty)
          values.asArray.get.foldLeft(initData) {
            case (x @ Left(_), _) => x
            case (Right(arr), n) =>
              n match {
                case value if value.isString => Right(arr :+ PString(value.asString.get))
                case value if value.isNumber =>
                  Right(
                    arr :+ PBigDecimal(
                      value.asNumber.flatMap(_.toBigDecimal).map(_.bigDecimal).get))
                case value if value.isBoolean => Right(arr :+ PBool(value.asBoolean.get))
                case a                        => Left(IllegalContractParams(Some(s"Not Support Json: ${a.noSpaces}")))
              }
          }

        case Right(x) => Left(IllegalContractParams(Some(s"Not Support Json: ${x.noSpaces}")))
      }
    }

    parseParams()
  }

  override def compileContractSourceCode(source: Path): Stack[Either[ContractCompileError, Path]] =
    Stack {
      val in  = Paths.get(source.toString, "src")
      val out = Paths.get(source.toString, "out")
      out.toFile.mkdirs()
      compiler.compileToNormalClasses(in, out).left.map(ContractCompileError.apply)
    }

  override def checkDeterministicOfClass(
      classFilePath: Path): Stack[Either[ContractCompileError, Path]] = Stack {
    compiler.checkDeterminism(classFilePath).left.map(ContractCompileError.apply)
  }

  override def jarContract(classFilePath: Path): Stack[BytesValue] = Stack {
    val baos                 = new ByteArrayOutputStream()
    val zip: ZipOutputStream = new ZipOutputStream(baos)

    val buffer = new Array[Byte](1024)
    @tailrec
    def copyStream(in: InputStream, out: OutputStream): Unit = {
      val readed = in.read(buffer)
      if (readed != -1) {
        out.write(buffer, 0, readed)
        copyStream(in, out)
      }
    }

    object FV extends SimpleFileVisitor[Path] {

      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        val f = file.toFile
        if (f.isFile && f.canRead) {
          val zipEntry =
            new ZipEntry(f.getAbsolutePath.substring(classFilePath.toString.length + 1))
          zip.putNextEntry(zipEntry)
          val input = new FileInputStream(f)
          copyStream(input, zip)
          zip.flush()
          input.close()

        }
        FileVisitResult.CONTINUE
      }
    }

    Files.walkFileTree(classFilePath, FV)
    zip.close()

    val bytes = baos.toByteArray
    baos.close()
    BytesValue(bytes)

  }

  override def resolveTransaction(transaction: Transaction): Stack[
    (Contract.Name, Contract.Version, Option[Contract.Function], Option[Contract.Parameter])] =
    Stack {
      import Contract.inner._
      transaction match {
        case x: Transaction.Transfer =>
          // to, amount
          val parameter = PArray(PString(x.to.value),
                                 PBigDecimal(java.math.BigDecimal.valueOf(x.amount.toBase.amount)))
          (TransferContract.name, TransferContract.version, None, Some(parameter))

        case x: Transaction.PublishContract =>
          // name, version, code, sig
          val parameter = PArray(
            PString(x.contract.owner.value),
            PString(x.contract.name.value),
            PString(x.contract.version.value),
            PString(x.contract.code.base64),
            PString(x.contract.codeSign.base64)
          )
          (PublishContract.name, PublishContract.version, None, Some(parameter))

        case x: Transaction.InvokeContract =>
          (x.name, x.version, Some(x.function), Some(x.parameter))
      }
    }

  //todo: now, not necessary to consider the cost of a transaction
  override def runContract(
      invoker: Account.ID,
      contract: Contract,
      function: Option[Contract.Function],
      currentStates: States,
      parameter: Option[Contract.Parameter]): Stack[Either[Throwable, StatesChange]] =
    Stack { setting =>
      import Contract.inner._
      contract match {
        case TransferContract =>
          parameter match {
            case Some(PArray(Array(PString(to), PBigDecimal(amount)))) =>
              // from - amount , to + amount
              val newStates = for {
                fromState <- currentStates.of(invoker.value)
                toState   <- currentStates.of(to)
              } yield {
                (fromState.withAmount(fromState.amount - amount),
                 toState.withAmount(toState.amount + amount))
              }

              // check newStates
              // 1. not None
              // 2. amount is gt 0
              if (newStates.isDefined) {
                val fromState = newStates.get._1
                if (fromState.amount < 0) Left(InsufficientBalance(fromState.accountId))
                else {
                  val changedStates = currentStates
                    .update(fromState)
                    .update(newStates.get._2)
                  Right(StatesChange(currentStates, changedStates))
                }
              } else Left(InsufficientTradePartners)

            case _ =>
              Left(IllegalContractParams("Invoking TransferContract Without Proper Parameters"))
          }

        case PublishContract =>
          //owner, name, version, code, sig
          parameter match {
            case Some(
                PArray(
                  Array(PString(owner),
                        PString(name),
                        PString(version),
                        PString(code),
                        PString(codeSig)))) =>
              // first, validate the sig
              //   @see: Contract#toBeHashed
              val source =
                BytesValue(new StringBuilder().append(name).append(version).append(code).toString())
              //MARK: validate signature
              val userContract = Contract.UserContract(Account.ID(owner),
                                                       Contract.Name(name),
                                                       Contract.Version(version),
                                                       Contract.Code(code),
                                                       Signature(BytesValue.decodeBase64(codeSig)))

              val validated = crypto.validateSignature(
                userContract.codeSign,
                userContract.toBeSigned,
                crypto.rebuildPubl(BytesValue.decodeHex(userContract.owner.value)))
              if (!validated) Left(ContractTampered(name, version))
              else {
                // then, save the contract to invoker's assets
                val ret: Option[Either[DecodingFailure, AccountState]] =
                  currentStates.of(invoker.value).map { accountState =>
                    val codeMapEl: Result[Map[String, ContractCodeItem]] = accountState
                      .assetOf(PublishContract.ASSET_NAME)
                      .map(new String(_, "UTF-8"))
                      .map(str => parse(str).right.get.as[Map[String, ContractCodeItem]])
                      .getOrElse(Right(Map.empty))

                    codeMapEl.right
                      .map { codeMap =>
                        codeMap + (crypto
                          .hash(BytesValue(name + version))
                          .hex -> ContractCodeItem(userContract.owner.value, name, version, code, userContract.codeSign.base64))
                      }
                      .map { items =>
                        accountState.updateAsset(PublishContract.ASSET_NAME,
                                                 items.asJson.noSpaces.getBytes("UTF-8"))
                      }
                  }
                ret match {
                  case None => Left(InsufficientTradePartners)
                  case Some(Left(t)) =>
                    logger.error("contract assets is broken", t)
                    Left(ContractAssetsBroken(invoker.value))
                  case Some(Right(contractAssets)) =>
                    Right(StatesChange(currentStates, currentStates.update(contractAssets)))

                }
              }

            case _ =>
              Left(IllegalContractParams("Invoking PublishContract Without Proper Parameters"))
          }
        case x: Contract.UserContract =>
          if (function.isEmpty)
            Left(IllegalContractInvoke(x.name.value, x.version.value, "no function to invoke"))
          else {
            val rand   = java.util.UUID.randomUUID().toString
            val tmpDir = Paths.get(setting.contractTempDir, rand)
            tmpDir.toFile.mkdirs()
            val jarFile = Paths.get(tmpDir.toString, s"${x.name.value}.jar")
            better.files
              .File(jarFile)
              .writeByteArray(BytesValue.decodeBase64(x.code.base64).bytes)

            // then extract jarFile
            better.files.File(jarFile).unzipTo(better.files.File(tmpDir))
            val contractMetaFile = better.files.File(s"$tmpDir/META-INF/contract")

            // contract config:
            // function1 = me.bigknife.test.TestContract#registerMyBanana
            val functionMap: Map[String, String] = contractMetaFile.lines
              .map(line =>
                line.split("=") match {
                  case Array(k, v) => Some((k.trim, v.trim))
                  case _           => None
              })
              .filter(_.isDefined)
              .map(_.get)
              .toMap
            if (functionMap.contains(function.get.name)) {

              functionMap(function.get.name).split("#") match {
                case Array(fullName, method) =>
                  val params: Seq[Any] = parameter
                    .map {
                      case PString(x0)     => Seq(x0)
                      case PBool(x0)       => Seq(x0)
                      case PBigDecimal(x0) => Seq(x0)
                      case PArray(array) =>
                        array.toSeq.map {
                          case PString(x1)     => x1
                          case PBool(x1)       => x1
                          case PBigDecimal(x1) => x1
                        }
                    }
                    .getOrElse(Seq.empty)
                  Runner.runAndInstrument(tmpDir,
                                          fullName,
                                          method,
                                          invoker.value,
                                          currentStates,
                                          params.map(_.asInstanceOf[AnyRef])) match {
                    case Left(t) =>
                      //delete tempDir
                      better.files.File(tmpDir).delete()
                      Left(ContractRuntimeError(x.name.value, x.version.value, t))
                    case Right((states, cost)) =>
                      //delete tempDir
                      better.files.File(tmpDir).delete()
                      logger.info(
                        s"run contract(name=${x.name.value}, version=${x.version.value}) cost = $cost")
                      Right(StatesChange(currentStates, states))
                  }
                case _ =>
                  //delete tempDir
                  better.files.File(tmpDir).delete()
                  Left(
                    IllegalContractInvoke(
                      x.name.value,
                      x.version.value,
                      s"function(${function.get.name} = ${functionMap(function.get.name)}) shape is illegal"))
              }

            } else {
              //delete tempDir
              better.files.File(tmpDir).delete()
              Left(
                IllegalContractInvoke(x.name.value,
                                      x.version.value,
                                      s"function(${function.get.name}) not found"))
            }
          }
      }
    }
}
object ContractServiceHandler {
  private val instance = new ContractServiceHandler
  trait Implicits {
    implicit val contractServiceHandler: ContractServiceHandler = instance
  }
  object implicits extends Implicits
}
