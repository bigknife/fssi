package fssi
package utils

import java.io.FileInputStream
import java.nio.file.{Path, Paths}

import org.objectweb.asm.{ClassReader, ClassWriter}

import scala.collection.mutable
import scala.util.Try

/**
  * Created on 2018/8/27
  */
class ContractClassLoader(classDir: Path) extends ClassLoader {
  val foundClasses: mutable.Map[String, Class[_]] = mutable.Map.empty

  override def findClass(name: String): Class[_] = {
    foundClasses.get(name) match {
      case Some(clazz) ⇒ clazz
      case None ⇒
        val clazz = Try { classOf[ContractClassLoader].getClassLoader.loadClass(name) }.toOption
          .orElse(Try { getClass.getClassLoader.loadClass(name) }.toOption)
          .map(_ ⇒ evaluateClass(name))
          .getOrElse(evaluateClassFile(name))
        foundClasses + (name → clazz)
        clazz
    }
  }

  private def evaluateClass(clazzName: String): Class[_] = {
    val cr = new ClassReader(clazzName)
    val cw = new ClassWriter(0)
    cr.accept(cw, 0)
    val bytes = cw.toByteArray
    defineClass(clazzName, bytes, 0, bytes.length)
  }

  private def evaluateClassFile(clazzName: String): Class[_] = {
    val classFile       = Paths.get(classDir.toString, s"${clazzName.replace(".", "/")}.class")
    val fileInputStream = new FileInputStream(classFile.toFile)
    val cr              = new ClassReader(fileInputStream)
    // TODO: add class file check
    val cw = new ClassWriter(0)
    cr.accept(cw, 0)
    val bytes = cw.toByteArray
    defineClass(clazzName, bytes, 0, bytes.length)
  }
}
