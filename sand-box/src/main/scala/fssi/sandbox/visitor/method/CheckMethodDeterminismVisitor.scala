package fssi
package sandbox
package visitor
package method

import org.objectweb.asm.{MethodVisitor, Opcodes, Type}

case class CheckMethodDeterminismVisitor(visitor: MethodVisitor,
                                         className: String,
                                         methodName: String,
                                         classDescriptor: String,
                                         track: scala.collection.mutable.ListBuffer[String])
    extends MethodVisitor(Opcodes.ASM6, visitor)
    with VisitorChecker {

  override def visitTypeInsn(opcode: Int, `type`: String): Unit = {
    super.visitTypeInsn(opcode, `type`)
    val _type = Type.getObjectType(`type`)
    if (isDescriptorForbidden(_type.getDescriptor)) {
      val methodDesc =
        if (methodName.startsWith("<init>")) "initialize member variable"
        else if (methodName.startsWith("<clinit>")) "initialize static variable"
        else s"initialize local variable in method [$className.$methodName]"
      track += s"$methodDesc of type [${_type.getClassName}] in class [$className] is forbidden"
    }
  }

  override def visitMethodInsn(opcode: Int,
                               owner: String,
                               name: String,
                               descriptor: String,
                               isInterface: Boolean): Unit = {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    if (!isDescriptorIgnored(classDescriptor)) {
      val ownerType = Type.getObjectType(owner)
      if (isDescriptorForbidden(ownerType.getDescriptor)) {
        track += s"invoke method [${ownerType.getClassName}.$name] in class [$className] is forbidden"
      }
    }
  }
}
