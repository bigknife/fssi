package fssi.sandbox

import org.objectweb.asm._
import Opcodes._
import org.slf4j.LoggerFactory

/**
  * @param mv
  */
class CostInstrumentingMethodVisitor(mv: MethodVisitor) extends MethodVisitor(ASM5, mv) {
  private val logger = LoggerFactory.getLogger(getClass)

  val runtimeAccounterTypeName = "org/wexchain/sandbox/java/CostAccounting"

  override def visitInsn(opcode: Int): Unit = {
    opcode match {
      case MONITORENTER => super.visitInsn(POP)
      case MONITOREXIT  => super.visitInsn(POP)
      case ATHROW =>
        try {
          super.visitMethodInsn(INVOKESTATIC, runtimeAccounterTypeName, "recordThrow", "()V", false)
        } catch {
          case a: Throwable =>
            logger.warn("record throw failed, ", a)
            super.visitInsn(opcode)
        }

      case _ => super.visitInsn(opcode)
    }
  }

  override def visitIntInsn(opcode: Int, operand: Int): Unit = {
    val typeSize: Int = (opcode, operand) match {
      case (NEWARRAY, T_BOOLEAN) | (NEWARRAY, T_BYTE) => 1
      case (NEWARRAY, T_SHORT) | (NEWARRAY, T_CHAR)   => 2
      case (NEWARRAY, T_INT) | (NEWARRAY, T_FLOAT)    => 4
      case (NEWARRAY, T_LONG) | (NEWARRAY, T_DOUBLE)  => 4
      case _                                          => 0
    }
    if (typeSize == 0) super.visitIntInsn(opcode, operand)
    else {
      super.visitInsn(DUP)
      super.visitLdcInsn(typeSize)
      super.visitMethodInsn(INVOKESTATIC,
                            runtimeAccounterTypeName,
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
                              runtimeAccounterTypeName,
                              "recordAllocation",
                              "(Ljava/lang/String;)V",
          false)

        super.visitTypeInsn(opcode, `type`)
      case ANEWARRAY =>
        super.visitInsn(DUP)
        super.visitLdcInsn(8)
        super.visitMethodInsn(INVOKESTATIC,
                              runtimeAccounterTypeName,
                              "recordArrayAllocation",
                              "(II)V",
          false)
        super.visitTypeInsn(opcode, `type`)
      case _ => super.visitTypeInsn(opcode, `type`)
    }
  }

  override def visitJumpInsn(opcode: Int, label: Label): Unit = {
    super.visitMethodInsn(INVOKESTATIC, runtimeAccounterTypeName, "recordJump", "()V", false)
    super.visitJumpInsn(opcode, label)
  }


  override def visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean): Unit = {
    opcode match {
      case INVOKEVIRTUAL | INVOKESTATIC | INVOKESPECIAL | INVOKEINTERFACE =>
        super.visitMethodInsn(INVOKESTATIC, runtimeAccounterTypeName, "recordMethodCall", "()V", isInterface)
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      case _ =>
        throw new IllegalStateException("Unexpected opcode: " + opcode + " from ASM when expecting an INVOKE")
    }
  }

}
