package fssi
package sandbox
package visitor

import org.objectweb.asm.{MethodVisitor, Opcodes, Type}

case class CheckMethodDeterminismVisitor(visitor: MethodVisitor,
                                         className: String,
                                         methodName: String,
                                         classDescriptor: String,
                                         track: scala.collection.mutable.ListBuffer[String])
    extends MethodVisitor(Opcodes.ASM6, visitor) {

  import Protocol._

  override def visitTypeInsn(opcode: Int, `type`: String): Unit = {
    super.visitTypeInsn(opcode, `type`)
    val _type = Type.getObjectType(`type`)
    forbiddenClasses.find(forbid => forbid.r.pattern.matcher(_type.getDescriptor).matches()) match {
      case Some(_) =>
        val methodDesc =
          if (methodName.startsWith("<init>")) "initialize member variable"
          else if (methodName.startsWith("<clinit>")) "initialize static variable"
          else s"initialize local variable in method [$className.$methodName]"
        track += s"$methodDesc of type [${_type.getClassName}] in class [$className] is forbidden"
      case None =>
    }
  }

  override def visitMethodInsn(opcode: Int,
                               owner: String,
                               name: String,
                               descriptor: String,
                               isInterface: Boolean): Unit = {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    val allowedOption =
      allowedClasses.find(allow => allow.r.pattern.matcher(classDescriptor).matches())
    if (allowedOption.isEmpty) {
      val ownerType = Type.getObjectType(owner)
      forbiddenClasses.find(forbid => forbid.r.pattern.matcher(ownerType.getDescriptor).matches()) match {
        case Some(_) =>
          track += s"invoke method [${ownerType.getClassName}.$name] in class [$className] is forbidden"
        case None =>
      }
    }
  }
}
