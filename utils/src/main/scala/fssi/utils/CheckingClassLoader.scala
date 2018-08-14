package fssi.utils

import java.io.FileInputStream
import java.nio.file.{Path, Paths}

import org.objectweb.asm._
import org.objectweb.asm.Opcodes._

import scala.collection.mutable.ListBuffer

/**
  * Created on 2018/8/14
  */
class CheckingClassLoader(val path: Path, val track: CheckingClassLoader.ClassCheckingStatus)
    extends ClassLoader {
  private lazy val cache: scala.collection.mutable.Map[String, Class[_]] =
    scala.collection.mutable.Map.empty

  override def findClass(name: String): Class[_] =
    try {
      if (cache.contains(name)) cache(name)
      else {
        val clazz = getClass.getClassLoader.loadClass(name)
        cache.put(name, clazz)
        val classReader       = new ClassReader(clazz.getName)
        val classCheckVisitor = CheckingClassLoader.ClassCheckingVisitor(null, this)
        classReader.accept(classCheckVisitor, ClassReader.SKIP_DEBUG)
        clazz
      }
    } catch {
      case Throwable ⇒
        cache.put(name, null)
        val absFile = Paths.get(path.toString, name.replaceAll("\\.", "/") + ".class").toFile
        if (!absFile.canRead) {
          track.addError(s"${absFile.getAbsolutePath} can't be read")
          null
        } else {
          val input             = new FileInputStream(absFile)
          val classWriter       = new ClassWriter(0)
          val classCheckVisitor = CheckingClassLoader.ClassCheckingVisitor(classWriter, this, true)
          val classReader       = new ClassReader(input)
          classReader.accept(classCheckVisitor, ClassReader.SKIP_DEBUG)
          null
        }
    }
}

object CheckingClassLoader {
  case class ClassCheckingStatus() {

    private lazy val errorBuffer = new scala.collection.mutable.ListBuffer[String]

    def addError(error: String): ClassCheckingStatus = {
      errorBuffer += error
      this
    }

    def addErrors(errors: Vector[String]): ClassCheckingStatus = {
      errorBuffer ++= errors
      this
    }

    def isLegal: Boolean       = errorBuffer.isEmpty
    def errors: Vector[String] = errorBuffer.toVector
  }

  case class ClassCheckVisitor(visitor: ClassVisitor,
                               checkClassLoader: CheckingClassLoader,
                               needCheckMethod: Boolean = false)
      extends ClassVisitor(ASM5, visitor) {
    import ClassCheckingVisitor._
    var currentClassName: String = _
    private val forbiddenClasses = Vector(
      "^Ljava/util/concurrent.*",
      "^Ljava/lang/refelect.*",
      "^Ljava/lang/Thread;",
      "^L/javax/.*",
      "^Lsun/.*"
    )

    override def visit(version: Int,
                       access: Int,
                       name: String,
                       signature: String,
                       superName: String,
                       interfaces: Array[String]): Unit = {
      currentClassName = NameUtils.innerNameToClassName(name)
      if (visitor != null) visitor.visit(version, access, name, signature, superName, interfaces)
      if (!visitedClasses.contains(currentClassName)) {
        visitedClasses += currentClassName
        val descOfName = NameUtils.innerNameToDescriptor(name)
        val errors     = scala.collection.mutable.ListBuffer.empty[String]
        forbiddenClasses.foldLeft(errors) { (acc, n) ⇒
          if (n.r.pattern.matcher(descOfName).matches())
            acc :+ s"$currentClassName:$name is forbidden"
          else acc
        }
        checkClassLoader.track.addErrors(errors.toVector)
        if (checkClassLoader.track.isLegal && superName != null) {
          val superClass = NameUtils.innerNameToClassName(superName)
          if (!visitedClasses.contains(superClass)) {
            visitedClasses += superClass
            checkClassLoader.findClass(superClass)
          }
        }
      }
    }

    override def visitField(access: Int,
                            name: String,
                            descriptor: String,
                            signature: String,
                            value: Any): FieldVisitor = {
      if (checkClassLoader.track.isLegal) {
        val errors = scala.collection.mutable.ListBuffer.empty[String]
        forbiddenClasses.foldLeft(errors) { (acc, n) ⇒
          if (n.r.pattern.matcher(descriptor).matches())
            acc += s"$currentClassName:$name of type ${NameUtils.descriptorToClassName(descriptor).get} is forbidden"
          else acc
        }
        checkClassLoader.track.addErrors(errors.toVector)

        if ((ACC_VOLATILE & access) == ACC_VOLATILE)
          checkClassLoader.track.addError(s"$currentClassName:$name, volatile is forbidden")
      }

      NameUtils.descriptorToClassName(descriptor) foreach { x ⇒
        if (!visitedClasses.contains(x)) checkClassLoader.loadClass(x)
      }

      null
    }

    override def visitMethod(access: Int,
                             name: String,
                             descriptor: String,
                             signature: String,
                             exceptions: Array[String]): MethodVisitor = {
      if ((access & ACC_SYNCHRONIZED) == ACC_SYNCHRONIZED || (access & ACC_NATIVE) == ACC_NATIVE && !currentClassName
            .startsWith("java")) {
        checkClassLoader.track.addError(
          s"$currentClassName:$name should not with access of 'abstract' 'synchronized' 'native'")
        super.visitMethod(access, name, descriptor, signature, exceptions)
      } else if (needCheckMethod && "<init>" != name) {
        val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (methodVisitor != null && !"<init>".equals(name)) MethodCheckingVisitor(checkClassLoader)
        else methodVisitor
      } else super.visitMethod(access, name, descriptor, signature, exceptions)
    }
  }

  object ClassCheckingVisitor {
    val visitedClasses: ListBuffer[String] = scala.collection.mutable.ListBuffer.empty[String]
  }

  case class MethodCheckingVisitor(checkingClassLoader: CheckingClassLoader)
      extends MethodVisitor(ASM5) {
    override def visitInsn(opcode: Int): Unit = super.visitInsn(opcode)
  }
}
