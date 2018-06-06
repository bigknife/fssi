package fssi.sandbox

import _root_.java.io.{File, FileInputStream, InputStream}
import org.objectweb.asm._
import org.objectweb.asm.Opcodes._
import scala.collection._

object CheckDeterminism {
  def checkClazz(clazz: Vector[File]): Vector[String] = {
    clazz.foldLeft(Vector.empty[String]) { (acc, n) =>
      val in = new FileInputStream(n)
      val r  = acc ++ check(in)
      in.close()
      r
    }
  }

  private def check(in: InputStream): Vector[String] = {
    val cl      = new ClassReader(in)
    val errors  = mutable.ListBuffer.empty[String]
    val visitor = new CheckVisitor(errors)
    cl.accept(visitor, ClassReader.SKIP_DEBUG)
    errors.toVector
  }

  /** check the class file if it has un-deterministic elements */
  class CheckVisitor(errors: mutable.ListBuffer[String]) extends ClassVisitor(ASM5) {
    var classDesc: String = _

    val forbiddenClasses = Vector(
      "^Ljava/util/concurrent.*"
    )

    override def visit(version: Int,
                       access: Int,
                       name: String,
                       signature: String,
                       superName: String,
                       interfaces: Array[String]): Unit = {
      classDesc = name.replaceAll("/", "\\.")
      super.visit(version, access, name, signature, superName, interfaces)
    }
    override def visitField(access: Int,
                            name: String,
                            descriptor: String,
                            signature: String,
                            value: scala.Any): FieldVisitor = {

      // println(s"${(ACC_VOLATILE & access) == ACC_VOLATILE} $name $descriptor $signature $value")
      /*
      // 不能有并发相关的东西
      if (descriptor.contains("Ljava/util/concurrent")) {
        errors += s"$classDesc:$name, can't be the type of java.util.concurrent.*"
      }
      // 不能有反射
      if (descriptor.contains("Ljava/lang/reflect")) {
        errors += s"$classDesc:$name, can't be the type of java.lang.reflect.*"
      }
      // 不能有线程
      if (descriptor.contains("Ljava/lang/Thread")) {
        errors += s"$classDesc:$name, can't be the type of java.lang.Thread"
      }
      // 不能有javax.tool相关的东西
      if (descriptor.contains("Ljavax/tools")) {
        errors += s"$classDesc:$name, can't be the type of javax.tools.*"
      }
      */
      forbiddenClasses.foldLeft(errors) {(acc, n) =>
        if (n.r.pattern.matcher(descriptor).matches()) {
          acc += s"$classDesc:$name is forbidden"
        }
        else acc
      }
      // 不能定义volatile
      if ((ACC_VOLATILE & access) == ACC_VOLATILE) {
        errors += s"$classDesc:$name, volatile is forbidden"
      }
      //静态变量也是不允许的
      if ((ACC_STATIC & access) == ACC_STATIC) {
        errors += s"$classDesc:$name, static is forbidden"
      }
      // transient 变量也是不允许的
      if ((ACC_TRANSIENT & access) == ACC_TRANSIENT) {
        errors += s"$classDesc:$name, transient is forbidden"
      }

      null
    }

  }
}
