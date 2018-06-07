package fssi.sandbox

import _root_.java.io.FileInputStream
import _root_.java.nio.file._

import fssi.sandbox.misc.NameUtils
import org.objectweb.asm.Opcodes._
import org.objectweb.asm._

/** a class loader for checking the class byte code
  *
  * @param path only load class file in the path
  */
class CheckingClassLoader(val path: Path, val track: CheckingClassLoader.ClassCheckingStatus)
    extends ClassLoader {
  private val cache: collection.mutable.Map[String, Class[_]] = collection.mutable.Map.empty

  override def findClass(name: String): Class[_] =
    try {
      if (cache.contains(name)) {
        cache(name)
      } else {
        val cls = getClass.getClassLoader.loadClass(name)
        cache.put(name, cls)
        val cr  = new ClassReader(cls.getName)
        val ccv = CheckingClassLoader.ClassCheckingVisitor(null, this)
        cr.accept(ccv, ClassReader.SKIP_DEBUG)
        cls
      }
    } catch {
      case e: Throwable =>
        cache.put(name, null)
        val absFile = Paths.get(path.toString, name.replaceAll("\\.", "/") + ".class").toFile
        if (!absFile.canRead) {
          track.addError(s"${absFile.getAbsolutePath} can't be read")
          // only for checking, there is no need to define a class.
          null
        } else {
          val fin = new FileInputStream(absFile)

          val cw  = new ClassWriter(0)
          val ccv = CheckingClassLoader.ClassCheckingVisitor(cw, this, needScanMethod = true)

          val cr = new ClassReader(fin)
          cr.accept(ccv, ClassReader.SKIP_DEBUG)
          // only for checking, there is no need to define a class.
          null
        }
    }
}

object CheckingClassLoader {
  case class ClassCheckingStatus() {
    import scala.collection._

    private val _errors: mutable.ListBuffer[String] = mutable.ListBuffer.empty[String]

    def addError(str: String): ClassCheckingStatus = {
      _errors += str
      this
    }

    def addErrors(xs: TraversableOnce[String]): ClassCheckingStatus = {
      _errors ++= xs
      this
    }

    def isLegal: Boolean         = _errors.isEmpty
    def errors(): Vector[String] = _errors.toVector
  }

  case class ClassCheckingVisitor(_cv: ClassVisitor,
                                  ccl: CheckingClassLoader,
                                  needScanMethod: Boolean = false)
      extends ClassVisitor(ASM5, _cv) {
    import ClassCheckingVisitor._
    var currentClassName: String = _
    private val forbiddenClasses = Vector(
      "^Ljava/util/concurrent.*",
      "^Ljava/lang/refelect.*",
      "^Ljava/lang/Thread;",
      "^Ljavax/.*",
      "^Lsun/.*"
    )

    override def visit(version: Int,
                       access: Int,
                       name: String,
                       signature: String,
                       superName: String,
                       interfaces: Array[String]): Unit = {

      currentClassName = NameUtils.innerNameToClassName(name)
      if (_cv != null) {
        _cv.visit(version, access, name, signature, superName, interfaces)
      }

      if (!visitedClass.contains(currentClassName)) {
        visitedClass += currentClassName
        //println(s"visiting: $name extends from $superName")
        val descOfName = NameUtils.innerNameToDescriptor(name)
        // check
        val errors = scala.collection.mutable.ListBuffer.empty[String]
        forbiddenClasses.foldLeft(errors) { (acc, n) =>
          if (n.r.pattern.matcher(descOfName).matches()) {
            acc += s"$currentClassName:$name is forbidden"
          } else acc
        }
        ccl.track.addErrors(errors.toVector)
        // if currentClassName is Forbidden, it's not necessary to check the super class
        if (ccl.track.isLegal && superName != null) {
          val superClass = NameUtils.innerNameToClassName(superName)
          if (!visitedClass.contains(superClass)) {
            visitedClass += superClass
            ccl.findClass(superClass)
          }
        }
      }
    }
    override def visitField(access: Int,
                            name: String,
                            descriptor: String,
                            signature: String,
                            value: scala.Any): FieldVisitor = {
      // if cl is illegal, it's not necessary to check the field.
      if (ccl.track.isLegal) {
        // check forbidden classes
        val errors = scala.collection.mutable.ListBuffer.empty[String]
        forbiddenClasses.foldLeft(errors) { (acc, n) =>
          if (n.r.pattern.matcher(descriptor).matches()) {
            acc += s"$currentClassName:$name of type ${NameUtils.descriptorToClassName(descriptor).get} is forbidden"
          } else {
            acc
          }
        }
        ccl.track.addErrors(errors)

        // volatile var is not permitted
        if ((ACC_VOLATILE & access) == ACC_VOLATILE) {
          ccl.track.addError(s"$currentClassName:$name, volatile is forbidden")
        }
        //todo think more about it.
        // static var is not permitted
        /*
        if ((ACC_STATIC & access) == ACC_STATIC) {
          ccl.track.addError(s"$currentClassName:$name, static is forbidden")
        }
        */
        // transient var is not permitted
        /*
        if ((ACC_TRANSIENT & access) == ACC_TRANSIENT) {
          ccl.track.addError(s"$currentClassName:$name, transient is forbidden")
        }
        */
      }

      //further more, load the class of the property to take a check.
      NameUtils.descriptorToClassName(descriptor) foreach { x =>
        if (!visitedClass.contains(x)) {
          // use ccl to load
          try {
            ccl.loadClass(x)
          } catch {
            case e => println(x + " " + e.getMessage)
          }
        }
      }

      null
    }

    override def visitMethod(access: Int,
                             name: String,
                             descriptor: String,
                             signature: String,
                             exceptions: Array[String]): MethodVisitor = {

      // check if native, synchronized
      if((/*(access & ACC_ABSTRACT) == ACC_ABSTRACT ||*/
        (access & ACC_SYNCHRONIZED) == ACC_SYNCHRONIZED ||
        (access & ACC_NATIVE) == ACC_NATIVE) && !currentClassName.startsWith("java")){
        ccl.track.addError(s"$currentClassName:$name should not with access of `abstract`, `synchronized` or `native`")
        super.visitMethod(access, name, descriptor, signature, exceptions)
      }
      else if (needScanMethod && "<init>" != name) {
        //println(s"name = $name, descriptor = $descriptor, sig = $signature, exceptions = $exceptions")
        val className = NameUtils.descriptorToClassName(descriptor)
        val mv        = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (mv != null && !"<init>".equals(name)) {
          MethodCheckingVisitor(ccl)
        } else mv
      } else super.visitMethod(access, name, descriptor, signature, exceptions)
    }
  }

  object ClassCheckingVisitor {
    var visitedClass: collection.mutable.ListBuffer[String] =
      collection.mutable.ListBuffer.empty[String]
  }
  case class MethodCheckingVisitor(ccl: CheckingClassLoader) extends MethodVisitor(ASM5) {
    override def visitInsn(opcode: Int): Unit = {
      super.visitInsn(opcode)
    }

    override def visitMethodInsn(opcode: Int,
                                 owner: String,
                                 name: String,
                                 descriptor: String,
                                 isInterface: Boolean): Unit = {
      //println(s"owner = $owner, name = $name, opcode = $opcode")
      if ("<init>" != name) {
        val className = NameUtils.innerNameToClassName(owner)
        ccl.loadClass(className)
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      } else super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)

    }

    override def visitTypeInsn(opcode: Int, `type`: String): Unit = {
      //println(s"opcode = $opcode, type=${`type`}")
      if (opcode == Opcodes.NEW) {
        // create a instance
        val className = NameUtils.innerNameToClassName(`type`)
        ccl.loadClass(className)
        //ccl.findClass(className)
      }
      super.visitTypeInsn(opcode, `type`)
    }
  }
}
