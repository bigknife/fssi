package fssi
package utils
import java.io.File
import java.nio.file.Path

trait FileUtil {

  def deleteDir(dir: java.nio.file.Path): Unit = {

    def deleteFile(file: java.io.File): Unit = {
      if (file.isDirectory) file.listFiles.foreach(child ⇒ deleteFile(child))
      file.delete(); ()
    }

    if (dir.toFile.exists()) deleteFile(dir.toFile)
    else ()
  }

  def findAllFiles(src: Path): Vector[File] = {
    def findFileByDir(dir: File, accFiles: Vector[File]): Vector[File] = dir match {
      case f if f.isFile => accFiles :+ f
      case d             => d.listFiles.toVector.foldLeft(accFiles)((f, v) => findFileByDir(v, f))
    }
    findFileByDir(src.toFile, Vector.empty)
  }
}

object FileUtil extends FileUtil
