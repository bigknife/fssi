package fssi
package interpreter

import types._
import exception._
import utils._
import trie._
import ast._
import java.io._
import java.nio.charset.Charset

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

  override def createDefaultConfigFile(chainRoot: File): Stack[File] = Stack {setting =>
    import better.files.{File => BFile, _}
    val str = Resource.getAsString("config-sample.conf")(Charset.forName("utf-8"))
    val f = new File(chainRoot, "fssi.conf")
    BFile(f.toPath).overwrite(str)
    f
  }
}

object ChainStoreHandler {
  private val instance = new ChainStoreHandler

  trait Implicits {
    implicit val chainStoreHandler: ChainStoreHandler = instance
  }
}
