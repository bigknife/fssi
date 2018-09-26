package fssi
package interpreter

import types._
import exception._
import utils._
import trie._
import ast._
import java.io._
import java.nio.charset.Charset

class ChainStoreHandler extends ChainStore.Handler[Stack] with LogSupport {

  /** create directory skeleton for a chain
    * if the root directory exists or is not empty, creation will fail.
    */
  override def createChainSkeleton(rootDir: File,
                                   chainID: String): Stack[Either[FSSIException, File]] =
    Stack {
      if (rootDir.exists)
        Left(new FSSIException(s"$rootDir is a file, can't be used as a chain root directory"))
      else {
        scala.util
          .Try {
            rootDir.mkdirs()
            log.debug(s"created chain root directory: $rootDir")
            // create a place holder file
            val placeHolder = new File(rootDir, ".chain")
            better.files.File(placeHolder.toPath).overwrite(chainID)
            log.debug(s"created chain placeholder file: $placeHolder")

            // now create 5 store directory: block/token/receipt/contract/data
            new File(rootDir, "block").mkdir()
            new File(rootDir, "token").mkdir()
            new File(rootDir, "contract").mkdir()
            new File(rootDir, "data").mkdir()
            new File(rootDir, "receipt").mkdir()
            log.debug("5 subdirectory created: block, token, contract, data, receipt")
            rootDir
          }
          .toEither
          .left
          .map(x => new FSSIException("create chain skeleton dir failed", Some(x)))
      }
    }

  override def createDefaultConfigFile(chainRoot: File): Stack[File] = Stack { setting =>
    import better.files.{File => BFile, _}
    val str = Resource.getAsString("config-sample.conf")(Charset.forName("utf-8"))
    val f   = new File(chainRoot, "fssi.conf")
    BFile(f.toPath).overwrite(str)
    f
  }

  /** return block store path
    */
  override def getBlockStoreRoot(chainRoot: File): Stack[File] = Stack {
    new File(chainRoot, "block")
  }

  /** return token store path
    */
  override def getTokenStoreRoot(chainRoot: File): Stack[File] = Stack {
    new File(chainRoot, "token")
  }

  /** return contract store path
    */
  override def getContractStoreRoot(chainRoot: File): Stack[File] = Stack {
    new File(chainRoot, "contract")
  }

  /** return data store path
    */
  override def getDataStoreRoot(chainRoot: File): Stack[File] = Stack {
    new File(chainRoot, "data")
  }

  /** return receipt store path
    */
  override def getReceiptStoreRoot(chainRoot: File): Stack[File] = Stack {
    new File(chainRoot, "receipt")
  }
}

object ChainStoreHandler {
  private val instance = new ChainStoreHandler

  trait Implicits {
    implicit val chainStoreHandler: ChainStoreHandler = instance
  }
}
