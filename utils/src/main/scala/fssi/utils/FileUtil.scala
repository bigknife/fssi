package fssi
package utils

trait FileUtil {

  def deleteDir(dir: java.nio.file.Path): Unit = {

    def deleteFile(file: java.io.File): Unit = {
      if (file.isDirectory) file.listFiles.foreach(child ⇒ deleteFile(child))
      file.delete(); ()
    }

    if (dir.toFile.exists()) deleteFile(dir.toFile)
    else ()
  }
}

object FileUtil extends FileUtil
