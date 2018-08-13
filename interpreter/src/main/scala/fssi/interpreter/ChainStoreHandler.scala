package fssi
package interpreter

import types._, exception._
import utils._, trie._
import ast._

import java.io._

class ChainStoreHandler extends ChainStore.Handler[Stack] {

  /** initialize a data directory to be a token store
    * @param dataDir directory to save token.
    */
  override def createChainRoot(dataDir: File, chainID: String): Stack[Either[FSSIException, File]] =
    Stack {

      if (dataDir.exists && dataDir.isFile)
        Left(new FSSIException(s"$dataDir is a file, can't be used as a chain root directory"))
      else {
        val chainRoot = new File(dataDir, chainID)
        if (chainRoot.exists && chainRoot.isFile)
          Left(new FSSIException(s"$dataDir is a file, can't be used as a chain root directory"))
        else {
          scala.util
            .Try {
              chainRoot.mkdirs()
              // create a place holder file
              val placeHolder = new File(chainRoot, ".chain")
              val fw          = new FileWriter(placeHolder)
              fw.write(chainID)
              fw.close
              chainRoot
            }
            .toEither
            .left
            .map(x => new FSSIException("create chain root dir failed", Some(x)))
        }
      }
    }
}

object ChainStoreHandler {
  private val instance = new ChainStoreHandler

  trait Implicits {
    implicit val chainStoreHandler: ChainStoreHandler = instance
  }
}
