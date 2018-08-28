package fssi
package utils

/**
  * Created on 2018/8/27
  */
trait FileUtil {

  def deleteDir(dir: java.nio.file.Path): Unit = {

    def deleteFile(file: java.io.File): Unit =
      if (file.isDirectory) file.listFiles.foreach(child ⇒ deleteFile(child))
      else file.delete()

    if (dir.toFile.exists()) deleteFile(dir.toFile)
    else ()
  }
}

object FileUtil extends FileUtil
