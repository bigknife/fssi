package fssi.utils

import java.io.FileInputStream
import java.nio.file.{Path, Paths}

import org.objectweb.asm.Opcodes._
import org.objectweb.asm._

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
        val classCheckVisitor = CheckingClassLoader.ClassCheckVisitor(null, this)
        classReader.accept(classCheckVisitor, ClassReader.SKIP_DEBUG)
        clazz
      }
    } catch {
      case _: Throwable ⇒
        cache.put(name, null)
        val classFile = Paths.get(path.toString, name.replaceAll("\\.", "/") + ".class").toFile
        if (!classFile.canRead) {
          track.addError(s"${classFile.getAbsolutePath} can't be read")
          null
        } else {
          val input       = new FileInputStream(classFile)
          val classWriter = new ClassWriter(0)
          val classCheckVisitor =
            CheckingClassLoader.ClassCheckVisitor(classWriter, this, needCheckMethod = true)
          val classReader = new ClassReader(input)
          classReader.accept(classCheckVisitor, ClassReader.SKIP_DEBUG)
          null
        }
    }
}

object CheckingClassLoader {
  lazy val forbiddenClasses = Vector(
    "^Ljava/util/concurrent.*",
    "^Ljava/lang/reflect.*",
    "^Ljava/lang/Thread;",
    "^Ljavax/.*",
    "^Lsun/.*",
    "^Ljava/net.*"
  )

  private def isContractInclude(className: String): Boolean = !className.startsWith("java")

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
      extends ClassVisitor(ASM6, visitor) {
    import ClassCheckingVisitor._
    var currentClassName: String = _

    override def visit(version: Int,
                       access: Int,
                       name: String,
                       signature: String,
                       superName: String,
                       interfaces: Array[String]): Unit = {
      currentClassName = Type.getObjectType(name).getClassName
      if (visitor != null) visitor.visit(version, access, name, signature, superName, interfaces)
      if (!visitedClasses.contains(currentClassName)) {
        visitedClasses += currentClassName
        val descOfName = Type.getObjectType(name).getDescriptor
        val errors     = scala.collection.mutable.ListBuffer.empty[String]
        forbiddenClasses.find(forbid ⇒ forbid.r.pattern.matcher(descOfName).matches()) match {
          case Some(_) ⇒ errors += s"class [$currentClassName] is forbidden"
          case None    ⇒
        }
        checkClassLoader.track.addErrors(errors.toVector)
        if (checkClassLoader.track.isLegal && superName != null) {
          val superClass = Type.getObjectType(superName).getClassName
          if (!visitedClasses.contains(superClass)) {
            visitedClasses += superClass
            checkClassLoader.findClass(superClass); ()
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
        forbiddenClasses.find(forbid ⇒ forbid.r.pattern.matcher(descriptor).matches()) match {
          case Some(_) ⇒
            errors += s"class field [$currentClassName.$name] of type [${Type.getType(descriptor).getClassName}] is forbidden"
          case None ⇒
        }
        checkClassLoader.track.addErrors(errors.toVector)
        if ((ACC_VOLATILE & access) == ACC_VOLATILE && isContractInclude(currentClassName))
          checkClassLoader.track.addError(
            s"class volatile field [$currentClassName.$name] is forbidden")
      }

      val className = Type.getType(descriptor).getClassName
      if (!visitedClasses.contains(className)) checkClassLoader.loadClass(className)
      null
    }

    override def visitMethod(access: Int,
                             name: String,
                             descriptor: String,
                             signature: String,
                             exceptions: Array[String]): MethodVisitor = {
      super.visitMethod(access, name, descriptor, signature, exceptions)
      if (!currentClassName.startsWith("java")) {
        if ((access & ACC_SYNCHRONIZED) == ACC_SYNCHRONIZED && isContractInclude(currentClassName))
          checkClassLoader.track.addError(
            s"class method [$currentClassName.$name] should not with access 'synchronized'")

        if ((access & ACC_NATIVE) == ACC_NATIVE && isContractInclude(currentClassName))
          checkClassLoader.track.addError(
            s"class method [$currentClassName.$name] should not with access 'native'")

      }
      MethodCheckingVisitor(currentClassName, name, checkClassLoader)
    }
  }

  object ClassCheckingVisitor {
    val visitedClasses: ListBuffer[String] = scala.collection.mutable.ListBuffer.empty[String]
  }

  case class MethodCheckingVisitor(className: String,
                                   methodName: String,
                                   checkingClassLoader: CheckingClassLoader)
      extends MethodVisitor(ASM6) {
    override def visitTypeInsn(opcode: Int, `type`: String): Unit = {
      super.visitTypeInsn(opcode, `type`)
      val _type  = Type.getObjectType(`type`)
      val errors = scala.collection.mutable.ListBuffer.empty[String]
      forbiddenClasses.find(forbid ⇒ forbid.r.pattern.matcher(_type.getDescriptor).matches()) match {
        case Some(_) ⇒
          errors += s"class method [$className.$methodName] local variable of type [${_type.getClassName}] is forbidden"
        case None ⇒
      }
      checkingClassLoader.track.addErrors(errors.toVector)
    }

    override def visitVarInsn(opcode: Int, `var`: Int): Unit =
      super.visitVarInsn(opcode, `var`)
  }
}
