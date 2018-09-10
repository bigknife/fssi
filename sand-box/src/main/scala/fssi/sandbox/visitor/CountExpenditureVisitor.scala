package fssi
package sandbox
package visitor
import org.objectweb.asm.{ClassVisitor, MethodVisitor}
import org.objectweb.asm.Opcodes._

class CountExpenditureVisitor(val visitor: ClassVisitor) extends ClassVisitor(ASM6, visitor) {

  override def visit(version: Int,
                     access: Int,
                     name: String,
                     signature: String,
                     superName: String,
                     interfaces: Array[String]): Unit = {
    if (visitor != null) visitor.visit(version, access, name, signature, superName, interfaces)
    else super.visit(version, access, name, signature, superName, interfaces)
  }

  override def visitMethod(access: Int,
                           name: String,
                           descriptor: String,
                           signature: String,
                           exceptions: Array[String]): MethodVisitor = {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    if (name == "<init>") mv
    else if (mv != null) new MethodCostAssessVisitor(mv)
    else mv
  }
}
