package fssi
package sandbox
package loader
import java.io.{File, FileInputStream}
import java.nio.file.{Path, Paths}

import fssi.sandbox.visitor.CountExpenditureVisitor
import org.objectweb.asm.{ClassReader, ClassWriter, Opcodes}

import scala.util.Try

class ContractClassLoader(path: Path) extends ClassLoader {

  val loadList = scala.collection.mutable.Map.empty[String, Class[_]]

  override def findClass(name: String): Class[_] = {
    if (loadList.contains(name)) loadList(name)
    else {
      val clazz = Try {
        classOf[ContractClassLoader].getClassLoader.loadClass(name)
      }.toOption.orElse {
        Try {
          getClass.getClassLoader.loadClass(name)
        }.toOption
      } match {
        case Some(_) => evaluateClass(name)
        case None =>
          val classFile = Paths.get(path.toString, s"${name.replaceAll("\\.", "/")}.class").toFile
          evaluateClassFile(classFile)
      }
      loadList += name -> clazz
      clazz
    }
  }

  private def evaluateClass(className: String): Class[_] = {
    val classReader  = new ClassReader(className)
    val classWriter  = new ClassWriter(0)
    val countVisitor = new CountExpenditureVisitor(classWriter)
    classReader.accept(countVisitor, 0)
    val array = classWriter.toByteArray
    defineClass(null, array, 0, array.length)
  }

  private def evaluateClassFile(classFile: File): Class[_] = {
    val classReader  = new ClassReader(new FileInputStream(classFile))
    val classWriter  = new ClassWriter(0)
    val countVisitor = new CountExpenditureVisitor(classWriter)
    classReader.accept(countVisitor, 0)
    val array = classWriter.toByteArray
    defineClass(null, array, 0, array.length)
  }
}
