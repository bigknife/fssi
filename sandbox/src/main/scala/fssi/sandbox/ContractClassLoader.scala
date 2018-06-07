package fssi.sandbox

import _root_.java.nio.file._
import _root_.java.io._

import org.objectweb.asm._
import fssi.sandbox.misc.NameUtils

import scala.util.Try
import scala.collection._

/**
  * a smart contract class loader
  */
class ContractClassLoader(contract: Path) extends ClassLoader {
  val finalClasses: mutable.Map[String, Class[_]] = mutable.Map.empty


  override def findClass(name: String): Class[_] = {
    if (finalClasses.contains(name)) {
      finalClasses(name)
    }else {
      // if system or parent class loader can load
      // evaluate it,then cache it.
      val evaluatedClass = Try {classOf[ContractClassLoader].getClassLoader.loadClass(name)}
        .toOption.orElse(Try{getClass.getClassLoader.loadClass(name)}.toOption)
        .map(_ => evaluateClass(name))
        .orElse(evaluateClassFile(name, contract))

      if (evaluatedClass.isDefined) {
        val cls = evaluatedClass.get
        finalClasses.put(name, cls)
        cls
      }else throw new ClassNotFoundException(s"$name can't be found")
    }

  }


  private def evaluateClass(name: String): Class[_] = {
    val cr = new ClassReader(name)
    val cw = new ClassWriter(0)
    val cv = new CostInstrumentingVisitor(cw)
    cr.accept(cv, 0)
    val bytes = cw.toByteArray
    defineClass(null, bytes, 0, bytes.length)
  }

  private def evaluateClassFile(name: String, zipFile: Path): Option[Class[_]] = {
    val classFile = Paths.get(zipFile.toString, s"${NameUtils.classNameToInnerName(name)}.class")
    val fis = new FileInputStream(classFile.toFile)
    val cr = new ClassReader(fis)
    val cw = new ClassWriter(0)
    val cv = new CostInstrumentingVisitor(cw)
    cr.accept(cv, 0)
    val bytes = cw.toByteArray
    Some(defineClass(null, bytes, 0, bytes.length))
  }
}
