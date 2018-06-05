package fssi.sandbox

import org.objectweb.asm.{ClassVisitor, MethodVisitor}
import org.objectweb.asm.Opcodes._

class CostInstrumentingVisitor(cv: ClassVisitor) extends ClassVisitor(ASM5, cv) {

  override def visit(version: Int,
                     access: Int,
                     name: String,
                     signature: String,
                     superName: String,
                     interfaces: Array[String]): Unit = {
    //super.visit(version, access, name, signature, superName, interfaces)
    cv.visit(version, access, name, signature, superName, interfaces)
  }

  override def visitMethod(access: Int,
                           name: String,
                           descriptor: String,
                           signature: String,
                           exceptions: Array[String]): MethodVisitor = {
    if("<init>" == name) {
      super.visitMethod(access, name, descriptor, signature, exceptions)
    }else{
      val mv = super.visitMethod(access,name, descriptor,signature, exceptions)
      if (mv != null) {
        new CostInstrumentingMethodVisitor(mv)
      }
      else mv
    }
  }
}
