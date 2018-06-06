package fssi.interpreter

import java.nio.file._

import fssi.ast.domain._
import fssi.ast.domain.exceptions.ContractCompileError
import fssi.ast.domain.types.{BytesValue, Contract}
import fssi.sandbox.compiler
import _root_.java.util.zip.{ZipEntry, ZipOutputStream}
import java.io._
import java.nio.file.attribute.BasicFileAttributes

import scala.annotation.tailrec

class ContractServiceHandler extends ContractService.Handler[Stack] {

  override def createContractWithoutSing(name: String,
                                         version: String,
                                         code: String): Stack[Contract] = Stack {
    Contract(
      Contract.Name(name),
      Contract.Version(version),
      Contract.Code(code)
    )
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
}
object ContractServiceHandler {
  trait Implicits {
    implicit val contractServiceHandler: ContractServiceHandler = new ContractServiceHandler
  }
  object implicits extends Implicits
}
