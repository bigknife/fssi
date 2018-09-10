package fssi
package sandbox
package visitor
import org.objectweb.asm.{Label, MethodVisitor}
import org.objectweb.asm.Opcodes._

class MethodCostAssessVisitor(methodVisitor: MethodVisitor)
    extends MethodVisitor(ASM6, methodVisitor) {

  import fssi.sandbox.types.Protocol._

  override def visitInsn(opcode: Int): Unit = {
    opcode match {
      case MONITORENTER => super.visitInsn(POP)
      case MONITOREXIT  => super.visitInsn(POP)
      case ATHROW =>
        super.visitMethodInsn(INVOKESTATIC, counterInternalName, "recordThrow", "()V", false)
        super.visitInsn(opcode)
      case _ => super.visitInsn(opcode)
    }
  }

  override def visitIntInsn(opcode: Int, operand: Int): Unit = {
    val typeSize = (opcode, operand) match {
      case (NEWARRAY, T_BOOLEAN) | (NEWARRAY, T_BYTE) => 1
      case (NEWARRAY, T_SHORT) | (NEWARRAY, T_CHAR)   => 2
      case (NEWARRAY, T_INT) | (NEWARRAY, T_FLOAT)    => 4
      case (NEWARRAY, T_LONG) | (NEWARRAY, T_DOUBLE)  => 8
      case _                                          => 0
    }
    if (typeSize == 0) super.visitIntInsn(opcode, operand)
    else {
      super.visitInsn(DUP)
      super.visitLdcInsn(typeSize)
      super.visitMethodInsn(INVOKESTATIC,
                            counterInternalName,
                            "recordArrayAllocation",
                            "(II)V",
                            false)
      super.visitIntInsn(opcode, operand)
    }
  }

  override def visitTypeInsn(opcode: Int, `type`: String): Unit = {
    opcode match {
      case NEW =>
        super.visitLdcInsn(`type`)
        super.visitMethodInsn(INVOKESTATIC,
                              counterInternalName,
                              "recordAllocation",
                              "(Ljava/lang/String;)V",
                              false)
        super.visitTypeInsn(opcode, `type`)

      case ANEWARRAY =>
        super.visitInsn(DUP)
        super.visitInsn(DUP)
        super.visitLdcInsn(8)
        super.visitMethodInsn(INVOKESTATIC,
                              counterInternalName,
                              "recordArrayAllocation",
                              "(II)V",
                              false)
        super.visitTypeInsn(opcode, `type`)

      case _ => super.visitTypeInsn(opcode, `type`)
    }
  }

  override def visitJumpInsn(opcode: Int, label: Label): Unit = {
    super.visitMethodInsn(INVOKESTATIC, counterInternalName, "recordJump", "()V", false)
    super.visitJumpInsn(opcode, label)
  }

  override def visitMethodInsn(opcode: Int,
                               owner: _root_.java.lang.String,
                               name: _root_.java.lang.String,
                               descriptor: _root_.java.lang.String,
                               isInterface: Boolean): Unit = {
    opcode match {
      case INVOKEVIRTUAL | INVOKESTATIC | INVOKESPECIAL | INVOKEINTERFACE =>
        super.visitMethodInsn(INVOKESTATIC, counterInternalName, "recordMethodCall", "()V", false)
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      case _ =>
        throw new IllegalStateException(
          s"Unexpected opcode: $opcode from ASM when expecting INVOKE")
    }
  }

  override def visitMaxs(maxStack: Int, maxLocals: Int): Unit =
    super.visitMaxs(maxStack + maxLocals, maxLocals)
}
