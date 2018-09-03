package fssi
package ast

import bigknife.sop._
import macros._
import implicits._
import java.nio.file.Path

import fssi.types.Contract.Parameter
import fssi.types._
import fssi.types.exception.ContractCompileError
import fssi.utils.BytesValue

@sp trait ContractService[F[_]] {

  /** compile source code to class file
    * @param path path of contract source file
    */
  def compileContract(path: Path): P[F, Either[ContractCompileError, Path]]

  /** check class file whether deterministic
    * @param contractPath path of compiled contract
    */
  def checkDeterminismOfContract(contractPath: Path): P[F, Either[ContractCompileError, Unit]]

  /** zip all of classes file to jar file
    * @param contractPath path of compiled contract
    */
  def zipContract(contractPath: Path): P[F, BytesValue]

  /** output zip file with format
    * @param bytesValue bytes value of class file
    * @param targetDir target path to store zip contract
    * @param format save format
    */
  def outputZipFile(bytesValue: BytesValue, targetDir: Path, format: CodeFormat): P[F, Unit]

  /** decode classes file
    * @param contractFile zip of classes
    * @param decodeFormat format to decode classes
    * @return classes byte values
    */
  def decodeContract(contractFile: Path, decodeFormat: CodeFormat): P[F, BytesValue]

  /** build contract from contract bytes value
    * @param bytesValue bytes value of contract
    * @return absolute path of contract dir
    */
  def rebuildContract(bytesValue: BytesValue): P[F, Path]

  /** check contract method whether legal
    * @param contractDir dir of tmp contract
    * @param className class name
    * @param methodName method name
    * @param parameters parameters for method
    */
  def checkContractMethod(contractDir: Path,
                          className: String,
                          methodName: String,
                          parameters: Array[Parameter]): P[F, Either[Throwable, Unit]]

  /** invoke concrete method in concrete class
    * @param contractDir dir of contract classes file
    * @param className  concrete class name
    * @param methodName concrete method name
    * @param parameters parameters for method
    */
  def invokeContractMethod(contractDir: Path,
                           className: String,
                           methodName: String,
                           parameters: Array[Parameter]): P[F, Either[Throwable, Unit]]
}
