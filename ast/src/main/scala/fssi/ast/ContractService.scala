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
  def outputZipFile(bytesValue: BytesValue, targetDir: Path, format: CodeFormat): P[F, Unit]

  /***
    * decode classes file
    * @param contractFile zip of classes
    * @param decodeFormat format to decode classes
    * @return classes byte values
    */
  def decodeContractClasses(contractFile: Path, decodeFormat: CodeFormat): P[F, BytesValue]

  /***
    * build contract dir by contract bytes value
    * @param bytesValue bytes value of contract
    * @return absolute path of contract dir
    */
  def buildContractDir(bytesValue: BytesValue): P[F, Path]

  /**
    * check contract method whether legal
    * @param contractDir dir of tmp contract
    * @param className class name
    * @param methodName method name
    */
  def checkContractMethod(contractDir: Path,
                          className: String,
                          methodName: String): P[F, Either[Throwable, Unit]]

  /***
    * invoke concrete method in concrete class
    * @param contractDir dir of contract classes file
    * @param className  concrete class name
    * @param methodName concrete method name
    * @param parameters parameters for method
    */
  def invokeContractMethod(contractDir: Path,
                           className: String,
                           methodName: String,
                           parameters: Array[String]): P[F, Either[Throwable, Unit]]
}
