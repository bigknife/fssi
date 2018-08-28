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
    "^Ljava/lang/Class;",
    "^Ljavax/.*",
    "^Lsun/.*",
    "^Ljava/net.*"
  )

  lazy val allowedClasses = Vector(
    "^Ljava/lang/Object;"
  )

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
    def errors: Vector[String] = errorBuffer.distinct.toVector
  }

  case class ClassCheckVisitor(visitor: ClassVisitor,
                               checkClassLoader: CheckingClassLoader,
                               needCheckMethod: Boolean = false)
      extends ClassVisitor(ASM6, visitor) {
    import ClassCheckingVisitor._
    var currentClassName: String       = _
    var currentClassDescriptor: String = _

    override def visit(version: Int,
                       access: Int,
                       name: String,
                       signature: String,
                       superName: String,
                       interfaces: Array[String]): Unit = {
      currentClassName = Type.getObjectType(name).getClassName
      currentClassDescriptor = Type.getObjectType(name).getDescriptor
      if (visitor != null) visitor.visit(version, access, name, signature, superName, interfaces)
      if (!visitedClasses.contains(currentClassName)) {
        visitedClasses += currentClassName
        forbiddenClasses.find(forbid ⇒ forbid.r.pattern.matcher(currentClassDescriptor).matches()) match {
          case Some(_) ⇒
            checkClassLoader.track.addError(s"class [$currentClassName] is forbidden")
          case None ⇒
        }
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
        forbiddenClasses.find(forbid ⇒ forbid.r.pattern.matcher(descriptor).matches()) match {
          case Some(_) ⇒
            checkClassLoader.track.addError(
              s"class field [$currentClassName.$name] of type [${Type.getType(descriptor).getClassName}] is forbidden")
          case None ⇒
        }
        if ((ACC_VOLATILE & access) == ACC_VOLATILE)
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
      val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
      if ((access & ACC_SYNCHRONIZED) == ACC_SYNCHRONIZED)
        checkClassLoader.track.addError(
          s"class method [$currentClassName.$name] should not with access 'synchronized'")

      if ((access & ACC_NATIVE) == ACC_NATIVE && !currentClassName.startsWith("java.lang.Object"))
        checkClassLoader.track.addError(
          s"class method [$currentClassName.$name] should not with access 'native'")

      MethodCheckingVisitor(currentClassName,
                            currentClassDescriptor,
                            name,
                            checkClassLoader,
                            methodVisitor)
    }

  }

  object ClassCheckingVisitor {
    val visitedClasses: ListBuffer[String] = scala.collection.mutable.ListBuffer.empty[String]
  }

  case class MethodCheckingVisitor(className: String,
                                   classDescriptor: String,
                                   methodName: String,
                                   checkingClassLoader: CheckingClassLoader,
                                   methodVisitor: MethodVisitor)
      extends MethodVisitor(ASM6, methodVisitor) {

    override def visitTypeInsn(opcode: Int, `type`: String): Unit = {
      super.visitTypeInsn(opcode, `type`)
      val _type = Type.getObjectType(`type`)
      forbiddenClasses.find(forbid ⇒ forbid.r.pattern.matcher(_type.getDescriptor).matches()) match {
        case Some(_) ⇒
          val methodDesc =
            if (methodName.startsWith("<init>")) "initialize member variable"
            else if (methodName.startsWith("<clinit>")) "initialize static variable"
            else s"initialize local variable in method [$className.$methodName]"
          checkingClassLoader.track.addError(
            s"$methodDesc of type [${_type.getClassName}] in class [$className] is forbidden")
        case None ⇒
      }
    }

    override def visitMethodInsn(opcode: Int,
                                 owner: String,
                                 name: String,
                                 descriptor: String,
                                 isInterface: Boolean): Unit = {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      val allowedOption =
        allowedClasses.find(allow ⇒ allow.r.pattern.matcher(classDescriptor).matches())
      if (allowedOption.isEmpty) {
        val ownerType = Type.getObjectType(owner)
        forbiddenClasses.find(forbid ⇒ forbid.r.pattern.matcher(ownerType.getDescriptor).matches()) match {
          case Some(_) ⇒
            checkingClassLoader.track.addError(
              s"invoke method [${ownerType.getClassName}.$name] in class [$className] is forbidden")
          case None ⇒
        }
      }
    }
  }
}
