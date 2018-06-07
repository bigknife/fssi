package fssi.interpreter

import java.nio.file._

import fssi.ast.domain._
import fssi.ast.domain.exceptions.{ContractCompileError, IllegalContractParams}
import fssi.ast.domain.types._
import fssi.sandbox.compiler
import _root_.java.util.zip.{ZipEntry, ZipOutputStream}
import java.io._
import java.nio.file.attribute.BasicFileAttributes

import fssi.ast.domain.types.Contract.Parameter.{PArray, PBigDecimal, PString}
import fssi.contract.States

import scala.annotation.tailrec

class ContractServiceHandler extends ContractService.Handler[Stack] {

  override def createContractWithoutSing(name: String,
                                         version: String,
                                         code: String): Stack[Contract.UserContract] = Stack {
    Contract.UserContract(
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

  override def resolveTransaction(transaction: Transaction)
    : Stack[(Contract.Name, Contract.Version, Option[Contract.Parameter])] = Stack {
    import Contract.inner._
    transaction match {
      case x: Transaction.Transfer =>
        // to, amount
        val parameter = PArray(PString(x.to.value),
                               PBigDecimal(java.math.BigDecimal.valueOf(x.amount.toBase.amount)))
        (TransferContract.name, TransferContract.version, Some(parameter))

      case x: Transaction.PublishContract =>
        // name, version, code, sig
        val parameter = PArray(
          PString(x.contract.name.value),
          PString(x.contract.version.value),
          PString(x.contract.code.base64),
          PString(x.signature.base64)
        )
        (PublishContract.name, PublishContract.version, Some(parameter))

      case x: Transaction.InvokeContract => (x.name, x.version, Some(x.parameter))
    }
  }

  override def runContract(invoker: Account,
                           contract: Contract,
                           currentStates: States,
                           parameter: Option[Contract.Parameter],
                           transactionId: Transaction.ID): Stack[Either[Throwable, Moment]] =
    Stack {
      import Contract.inner._
      contract match {
        case TransferContract         => ???
        case PublishContract          => ???
        case x: Contract.UserContract => ???
      }
    }
}
object ContractServiceHandler {
  trait Implicits {
    implicit val contractServiceHandler: ContractServiceHandler = new ContractServiceHandler
  }
  object implicits extends Implicits
}
