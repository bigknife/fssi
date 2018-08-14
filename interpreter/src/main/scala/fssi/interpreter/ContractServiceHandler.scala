package fssi.interpreter

import java.io._
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import java.util.zip.{ZipEntry, ZipOutputStream}

import fssi.ast.ContractService
import fssi.types.OutputFormat.{Base64, Hex, Jar}
import fssi.types._
import fssi.types.exception._
import fssi.utils.{Compiler => compiler}

/**
  * Created on 2018/8/14
  */
class ContractServiceHandler extends ContractService.Handler[Stack] {

  override def compileContractSourceCode(
      sourcePath: Path): Stack[Either[ContractCompileError, Path]] = Stack {
    val in  = Paths.get(sourcePath.toString, "src")
    val out = Paths.get(sourcePath.toString, "out")
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
    val array = output.toByteArray
    zipOut.flush(); output.flush(); zipOut.close(); output.close()
    BytesValue(array)
  }

  override def outputZipFile(bytesValue: BytesValue,
                             targetDir: Path,
                             format: OutputFormat): Stack[Unit] = Stack {
    def writeBytes(f: Array[Byte]): Unit =
      better.files.File(targetDir.toString).outputStream.map(os ⇒ os.write(f)).map(_=> ())

    format match {
      case Jar    ⇒ writeBytes(bytesValue.value)
      case Hex    ⇒ writeBytes(bytesValue.toHexString.bytes)
      case Base64 ⇒ writeBytes(bytesValue.toBase64String.bytes)
    }
  }
}

object ContractServiceHandler {
  val instance = new ContractServiceHandler

  trait Implicits {
    implicit lazy val contractServiceHandler: ContractServiceHandler = instance
  }
}
