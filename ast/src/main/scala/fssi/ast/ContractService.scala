package fssi.ast

import bigknife.sop._
import macros._
import implicits._
import java.io._
import java.nio.file.Path

import fssi.types._
import fssi.types.exception.ContractCompileError

/**
  * Created on 2018/8/14
  */
@sp trait ContractService[F[_]] {

  /***
    * compile source code to class file
    * @param sourcePath path of source file
    */
  def compileContractSourceCode(sourcePath: Path): P[F, Either[ContractCompileError, Path]]

  /***
    * check class file whether deterministic
    * @param classFilePath class file path
    */
  def checkDeterministicOfClass(classFilePath: Path): P[F, Either[ContractCompileError, Unit]]

  /***
    * zip all of classes file to jar file
    * @param classFilePath path of class file
    * @param destPath dest path to store jar
    */
  def zipContract(classFilePath: Path): P[F, BytesValue]

  /***
    * output zip file with format
    * @param bytesValue bytes value of class file
    * @param format save format
    */
  def outputZipFile(bytesValue: BytesValue, targetDir: Path, format: OutputFormat): P[F, Unit]
}
