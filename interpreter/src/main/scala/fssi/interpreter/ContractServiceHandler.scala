package fssi
package interpreter

import java.io._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import java.util.zip.{ZipEntry, ZipOutputStream}

import fssi.interpreter.infr.{Compiler => compiler}
import fssi.ast.ContractService
import fssi.interpreter.Setting.ToolSetting
import fssi.interpreter.infr.CheckingClassLoader
import fssi.types.CodeFormat.{Base64, Hex, Jar}
import fssi.types.Contract.Parameter
import fssi.types.Contract.Parameter.PrimaryParameter
import fssi.types._
import fssi.types.exception._
import fssi.utils.{BytesUtil, FileUtil}

import scala.util.Try

/**
  * Created on 2018/8/14
  */
class ContractServiceHandler extends ContractService.Handler[Stack] {

  override def compileContractSourceCode(
      sourcePath: Path): Stack[Either[ContractCompileError, Path]] = Stack {
    val in      = Paths.get(sourcePath.toString, "src")
    val out     = Paths.get(sourcePath.toString, "out")
    val outFile = out.toFile
    if (outFile.exists()) FileUtil.deleteDir(out)
    out.toFile.mkdirs()
    compiler.compileToNormalClass(in, out).left.map(ContractCompileError)
  }

  override def checkDeterministicOfClass(
      classFilePath: Path): Stack[Either[ContractCompileError, Unit]] =
    Stack(compiler.checkDeterminism(classFilePath).left.map(ContractCompileError))

  override def zipContract(classFilePath: Path): Stack[BytesValue] = Stack {
    val output = new ByteArrayOutputStream()
    val zipOut = new ZipOutputStream(output)
    val buffer = new Array[Byte](1024)
    object FV extends SimpleFileVisitor[Path] {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        val f = file.toFile
        if (f.isFile && f.canRead) {
          val entry = new ZipEntry(f.getAbsolutePath.substring(classFilePath.toString.length + 1))
          val input = new BufferedInputStream(new FileInputStream(f))
          zipOut.putNextEntry(entry)
          Iterator
            .continually(input.read(buffer))
            .takeWhile(_ != -1)
            .foreach(read ⇒ zipOut.write(buffer, 0, read))
          zipOut.flush()
          zipOut.closeEntry()
          input.close()
        }
        FileVisitResult.CONTINUE
      }
    }
    Files.walkFileTree(classFilePath, FV)
    zipOut.flush(); output.flush(); zipOut.close()
    val array = output.toByteArray; output.close(); FileUtil.deleteDir(classFilePath)
    BytesValue(array)
  }

  override def outputZipFile(bytesValue: BytesValue,
                             targetDir: Path,
                             format: CodeFormat): Stack[Unit] = Stack {
    def write(bytes: Array[Byte]): Unit = {
      better.files.File(targetDir.toString).outputStream.map(os ⇒ os.write(bytes)).get()
    }

    if (!targetDir.toFile.exists()) targetDir.toFile.createNewFile()
    format match {
      case Jar    ⇒ write(bytesValue.value)
      case Hex    ⇒ write(bytesValue.toHexString.toString().getBytes("utf-8"))
      case Base64 ⇒ write(bytesValue.toBase64String.toString().getBytes("utf-8"))
    }
  }

  override def decodeContractClasses(contractFile: Path,
                                     decodeFormat: CodeFormat): Stack[BytesValue] = Stack {
    val bytes = better.files.File(contractFile).byteArray
    val decodeBytes = decodeFormat match {
      case Jar    ⇒ bytes
      case Hex    ⇒ HexString.decode(new String(bytes, "utf-8")).bytes
      case Base64 ⇒ BytesUtil.decodeBase64(new String(bytes, "utf-8"))
    }
    BytesValue(decodeBytes)
  }

  override def buildContractDir(bytesValue: BytesValue): Stack[Path] = Stack { setting ⇒
    val rand   = UUID.randomUUID().toString.replace("-", "")
    val tmpDir = Paths.get(s"${setting.asInstanceOf[ToolSetting].contractTempDir}", rand)
    tmpDir.toFile.mkdirs()
    val jarFile = Paths.get(tmpDir.toString, s"$rand.jar")
    better.files.File(jarFile).writeByteArray(bytesValue.value)
    better.files.File(jarFile).unzipTo(tmpDir)
    if (jarFile.toFile.exists()) jarFile.toFile.delete()
    tmpDir
  }

  override def checkContractMethod(contractDir: Path,
                                   className: String,
                                   methodName: String,
                                   parameters: Array[Parameter]): Stack[Either[Throwable, Unit]] =
    Stack {
      Try {
        val metaFile = Paths.get(contractDir.toString, "META-INF/contract")
        val existed = better.files.File(metaFile).lines.toVector.exists { line ⇒
          line.split("\\s*=\\s*") match {
            case Array(_, qualifiedMethodName) ⇒
              qualifiedMethodName.split("#") match {
                case Array(clazz, method) ⇒
                  val leftIndex  = method.indexOf("(")
                  val rightIndex = method.lastIndexOf(")")
                  if (leftIndex < 0 || rightIndex < 0)
                    throw new IllegalArgumentException(s"smart contract invalid: $line")
                  else {
                    val methodName = method.substring(0, leftIndex)
                    val args =
                      method.substring(leftIndex + 1, rightIndex).split(",").filter(_.nonEmpty)
                    clazz == className && methodName == methodName && (args sameElements parameters
                      .map(_.`type`.`type`.getSimpleName))
                  }
                case _ ⇒ false
              }
            case _ ⇒ false
          }
        }
        if (existed) ()
        else {
          FileUtil.deleteDir(contractDir)
          throw new NoSuchMethodException(
            s"contract method [$className#$methodName(${parameters.map(_.`type`.`type`.getSimpleName).mkString(",")})] not found")
        }
      }.toEither
    }

  override def invokeContractMethod(contractDir: Path,
                                    className: String,
                                    methodName: String,
                                    parameters: Array[Parameter]): Stack[Either[Throwable, Unit]] =
    Stack {
      try {
        val track       = CheckingClassLoader.ClassCheckingStatus()
        val classLoader = new CheckingClassLoader(contractDir, track)
        val clazz =
          classLoader.findClassMethod(className, methodName, parameters.map(_.`type`.`type`))
        if (clazz == null)
          Left(new ClassNotFoundException(
            s"can not find class method [$className#$methodName(${parameters.map(_.`type`.`type`.getSimpleName).mkString(",")})]"))
        else {
          if (track.isLegal) {
            val method   = clazz.getMethod(methodName, parameters.map(_.`type`.`type`): _*)
            val instance = clazz.newInstance()
            method.invoke(
              instance,
              parameters.map(_.asInstanceOf[PrimaryParameter].value.asInstanceOf[AnyRef]): _*)
            Right(())
          } else Left(ContractCompileError(track.errors))
        }
      } catch {
        case e: Throwable ⇒ Left(e)
      } finally FileUtil.deleteDir(contractDir)
    }
}

object ContractServiceHandler {
  val instance = new ContractServiceHandler

  trait Implicits {
    implicit lazy val contractServiceHandler: ContractServiceHandler = instance
  }
}
