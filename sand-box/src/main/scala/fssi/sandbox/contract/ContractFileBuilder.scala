package fssi
package sandbox
package contract
import java.io.{File, RandomAccessFile}
import java.nio.charset.Charset

import fssi.sandbox.exception.ContractBuildException
import fssi.sandbox.inf.BaseLogger
import fssi.types.exception.FSSIException
import fssi.utils._

import scala.util.Try

class ContractFileBuilder extends BaseLogger {

  crypto.registerBC()

  def makeContractSignature(privateKey: Array[Byte],
                            contractBytes: Array[Byte]): Either[FSSIException, Array[Byte]] = {
    logger.debug(s"make signature for contract ,private key length: ${privateKey.length}")
    Try {
      crypto.makeSignature(contractBytes, crypto.rebuildECPrivateKey(privateKey, crypto.SECP256K1))
    }.toEither.left
      .map(x => new FSSIException(x.getMessage))
  }

  def verifyContractSignature(publicKey: Array[Byte],
                              source: Array[Byte],
                              contractSignature: Array[Byte]): Either[FSSIException, Unit] = {
    logger.debug(s"verify signature for contract ,public key length: ${publicKey.length}")
    Try {
      val r =
        crypto.verifySignature(contractSignature,
                               source,
                               crypto.rebuildECPublicKey(publicKey, crypto.SECP256K1))
      if (r) ()
      else throw new FSSIException("contract signature verified failed")
    }.toEither.left
      .map(x => new FSSIException(x.getMessage))
  }

  def addContractMagic(contractFile: File): Either[FSSIException, Unit] = {
    import fssi.sandbox.types.Protocol._
    logger.debug(s"add magic $magic to contract file: $contractFile")
    if (contractFile.exists() && contractFile.isFile) {
      try {
        val randomAccessFile = new RandomAccessFile(contractFile, "rw")
        randomAccessFile.seek(0)
        randomAccessFile.writeBytes(magic)
        randomAccessFile.close()
        Right(())
      } catch {
        case t: Throwable =>
          val error = s"add magic $magic to contract file: $contractFile failed: ${t.getMessage}"
          Left(ContractBuildException(Vector(error)))
      }
    } else Left(ContractBuildException(Vector(s"contract file $contractFile not found")))
  }

  def addContractSize(size: Long, contractFile: File): Either[FSSIException, Unit] = {
    import fssi.sandbox.types.Protocol._
    logger.debug(s"add contract size $size to contract file: $contractFile")
    if (contractFile.exists() && contractFile.isFile) {
      try {
        val randomAccessFile = new RandomAccessFile(contractFile, "rw")
        randomAccessFile.seek(magic.length.toLong)
        randomAccessFile.writeLong(size)
        randomAccessFile.close()
        Right(())
      } catch {
        case t: Throwable =>
          val error = s"add size $size to contract file: $contractFile failed: ${t.getMessage}"
          Left(ContractBuildException(Vector(error)))
      }
    } else Left(ContractBuildException(Vector(s"contract file $contractFile not found")))
  }

  def addSmartContract(contractBytes: Array[Byte],
                       contractFile: File): Either[FSSIException, Unit] = {
    logger.debug(s"add contract ${contractBytes.length} bytes to contract file: $contractFile")
    import fssi.sandbox.types.Protocol._
    if (contractFile.exists() && contractFile.isFile) {
      try {
        val randomAccessFile = new RandomAccessFile(contractFile, "rw")
        randomAccessFile.seek(magic.length.toLong + 8)
        randomAccessFile.write(contractBytes, 0, contractBytes.length)
        randomAccessFile.close()
        Right(())
      } catch {
        case t: Throwable =>
          val error =
            s"add contract body ${contractBytes.length} bytes to contract file: $contractFile failed: ${t.getMessage}"
          Left(ContractBuildException(Vector(error)))
      }
    } else Left(ContractBuildException(Vector(s"contract file $contractFile not found")))
  }

  def addContractSignature(signature: Array[Byte],
                           contractFile: File): Either[FSSIException, Unit] = {
    logger.debug(
      s"add contract signature ${signature.length} bytes to contract file: $contractFile")
    import fssi.sandbox.types.Protocol._
    if (contractFile.exists() && contractFile.isFile) {
      try {
        val randomAccessFile = new RandomAccessFile(contractFile, "rw")
        randomAccessFile.seek(magic.length.toLong)
        val length = randomAccessFile.readLong()
        randomAccessFile.seek(magic.length.toLong + 8 + length)
        randomAccessFile.write(signature, 0, signature.length)
        Right(())
      } catch {
        case t: Throwable =>
          val error =
            s"add contract signature ${signature.length} bytes to contract file: $contractFile failed: ${t.getMessage}"
          Left(ContractBuildException(Vector(error)))
      }
    } else Left(ContractBuildException(Vector(s"contract file $contractFile not found")))
  }

  def readContractMagic(contractFile: File): Either[FSSIException, Unit] = {
    logger.debug(s"read magic from contract file: $contractFile")
    import fssi.sandbox.types.Protocol._
    if (contractFile.exists() && contractFile.isFile) {
      Try {
        val randomAccessFile = new RandomAccessFile(contractFile, "rw")
        randomAccessFile.seek(0)
        val magicBytes = new Array[Byte](magic.length)
        randomAccessFile.read(magicBytes, 0, magicBytes.length)
        require(new String(magicBytes, 0, magicBytes.length, Charset.forName("utf-8")) == magic,
                s"sandbox smart contract must be $magic")
      }.toEither.left.map(x => new FSSIException(x.getMessage))
    } else Left(new FSSIException(s"contract file $contractFile not found"))
  }

  def readContractSize(contractFile: File): Either[FSSIException, Long] = {
    logger.debug(s"read contract size from contract file: $contractFile")
    import fssi.sandbox.types.Protocol._
    if (contractFile.exists() && contractFile.isFile) {
      Try {
        val randomAccessFile = new RandomAccessFile(contractFile, "rw")
        randomAccessFile.seek(magic.length.toLong)
        randomAccessFile.readLong()
      }.toEither.left.map(x => new FSSIException(x.getMessage))
    } else Left(new FSSIException(s"contract file $contractFile not found"))
  }

  def readSmartContract(contractFile: File, size: Long): Either[FSSIException, Array[Byte]] = {
    logger.debug(s"read smart contract from contract file: $contractFile")
    import fssi.sandbox.types.Protocol._
    if (contractFile.exists() && contractFile.isFile) {
      Try {
        val randomAccessFile = new RandomAccessFile(contractFile, "rw")
        randomAccessFile.seek(magic.length.toLong + 8)
        val contractBytes = new Array[Byte](size.toInt)
        randomAccessFile.read(contractBytes, 0, contractBytes.length)
        contractBytes
      }.toEither.left.map(x => new FSSIException(x.getMessage))
    } else Left(new FSSIException(s"contract file $contractFile not found"))
  }

  def readContractSignature(contractFile: File,
                            smartContractSize: Long): Either[FSSIException, Array[Byte]] = {
    logger.debug(s"read contract signature from contract file: $contractFile")
    import fssi.sandbox.types.Protocol._
    if (contractFile.exists() && contractFile.isFile) {
      Try {
        val randomAccessFile = new RandomAccessFile(contractFile, "rw")
        val forwardSize      = magic.length + 8 + smartContractSize
        randomAccessFile.seek(forwardSize)
        val signatureBytes = new Array[Byte](contractFile.length().toInt - forwardSize.toInt)
        randomAccessFile.read(signatureBytes, 0, signatureBytes.length)
        signatureBytes
      }.toEither.left.map(x => new FSSIException(x.getMessage))
    } else
      Left(new FSSIException(s"contract file $contractFile not found"))
  }
}
