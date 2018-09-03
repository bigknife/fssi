package fssi
package sandbox
package visitor

import org.objectweb.asm.{ClassVisitor, Opcodes}

case class UpgradeClassMagicVisitor(visitor: ClassVisitor, sandBoxVersion: SandBoxVersion)
    extends ClassVisitor(Opcodes.ASM6, visitor) {
  override def visit(version: Int,
                     access: Int,
                     name: String,
                     signature: String,
                     superName: String,
                     interfaces: Array[_root_.java.lang.String]): Unit = {
    if (sandBoxVersion.isVersionValid) {
      val innerVersion = sandBoxVersion.toInnerVersion.value
      if (visitor != null)
        visitor.visit(innerVersion, access, name, signature, superName, interfaces)
      else super.visit(innerVersion, access, name, signature, superName, interfaces)
    }
  }
}
