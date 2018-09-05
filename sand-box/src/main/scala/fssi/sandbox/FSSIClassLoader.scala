package fssi
package sandbox

import java.io.{File, FileInputStream}
import java.nio.file.{Path, Paths}

import fssi.sandbox.visitor._
import org.objectweb.asm._

class FSSIClassLoader(val path: Path, val track: scala.collection.mutable.ListBuffer[String])
    extends ClassLoader {
  private lazy val cache: scala.collection.mutable.Map[String, Class[_]] =
    scala.collection.mutable.Map.empty

  def findClassFromLoader(name: String,
                          methodName: String,
                          parameterTypes: Array[Class[_]]): Class[_] = {
    if (cache.contains(name) && methodName.isEmpty) cache(name)
    else {
      val clazz       = getClass.getClassLoader.loadClass(name)
      val classReader = new ClassReader(clazz.getName)
      val classCheckVisitor =
        CheckClassDeterminismVisitor(this,
                                     null,
                                     track,
                                     name,
                                     methodName,
                                     parameterTypes.map(_.getName),
                                     methodName.nonEmpty)
      classReader.accept(classCheckVisitor, ClassReader.SKIP_DEBUG)
      if (!cache.contains(name)) cache.put(name, clazz)
      clazz
    }
  }

  def findClassFromClassFile(classFile: File,
                             name: String,
                             methodName: String,
                             parameterTypes: Array[Class[_]]): Class[_] = {
    if (cache.contains(name) && methodName.isEmpty) cache(name)
    else {
      if (!classFile.canRead) {
        track += s"${classFile.getAbsolutePath} can't be read"
        null
      } else {
        val input       = new FileInputStream(classFile)
        val classWriter = new ClassWriter(0)
        val classCheckVisitor = CheckClassDeterminismVisitor(this,
                                                             classWriter,
                                                             track,
                                                             name,
                                                             methodName,
                                                             parameterTypes.map(_.getName),
                                                             methodName.nonEmpty)
        val classReader = new ClassReader(input)
        classReader.accept(classCheckVisitor, ClassReader.SKIP_DEBUG)
        val bytes = classWriter.toByteArray
        if (!cache.contains(name)) {
          val clazz = defineClass(name, bytes, 0, bytes.length)
          cache.put(name, clazz)
          clazz
        } else cache(name)
      }
    }
  }

  def findClass(name: String, methodName: String, parameterTypes: Array[Class[_]]): Class[_] =
    try {
      findClassFromLoader(name, methodName, parameterTypes)
    } catch {
      case _: Throwable =>
        val classFile = Paths.get(path.toString, name.replaceAll("\\.", "/") + ".class").toFile
        findClassFromClassFile(classFile, name, methodName, parameterTypes)
    }
}
