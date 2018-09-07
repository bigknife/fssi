package fssi
package sandbox
package loader
import java.io.FileInputStream
import java.nio.file.{Path, Paths}

import org.objectweb.asm.{ClassReader, ClassWriter}

import scala.util.Try

class ContractClassLoader(path: Path) extends ClassLoader {

  val loadList = scala.collection.mutable.Map.empty[String, Class[_]]

  override def findClass(name: String): Class[_] = {
    if (loadList.contains(name)) loadList(name)
    else {
      Try {
        classOf[ContractClassLoader].getClassLoader.loadClass(name)
      }.toOption.orElse {
        Try {
          getClass.getClassLoader.loadClass(name)
        }.toOption
      } match {
        case Some(clazz) =>
          loadList += name -> clazz
          clazz
        case None =>
          val classFile   = Paths.get(path.toString, s"${name.replaceAll("\\.", "/")}.class").toFile
          val classReader = new ClassReader(new FileInputStream(classFile))
          val classWriter = new ClassWriter(classReader, 0)
          classReader.accept(classWriter, 0)
          val array = classWriter.toByteArray
          val clazz = defineClass(name, array, 0, array.length)
          loadList += name -> clazz
          clazz
      }
    }
  }
}
