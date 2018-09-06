package fssi
package sandbox
package visitor

import org.objectweb.asm.{ClassVisitor, Opcodes}

case class UpgradeClassVersionVisitor(visitor: ClassVisitor, sandBoxVersion: SandBoxVersion)
    extends ClassVisitor(Opcodes.ASM6, visitor) {
  override def visit(version: Int,
                     access: Int,
                     name: String,
                     signature: String,
                     superName: String,
                     interfaces: Array[_root_.java.lang.String]): Unit = {
    val boxVersion = sandBoxVersion.toInnerVersion
    if (boxVersion.isVersionValid) {
      if (visitor != null)
        visitor.visit(boxVersion.value, access, name, signature, superName, interfaces)
      else super.visit(boxVersion.value, access, name, signature, superName, interfaces)
    } else
      throw new IllegalArgumentException(s"sandbox version ${sandBoxVersion.value} is not support")
  }
}
