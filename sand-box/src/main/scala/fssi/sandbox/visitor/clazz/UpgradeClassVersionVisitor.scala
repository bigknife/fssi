package fssi
package sandbox
package visitor
package clazz
import fssi.sandbox.types.SandBoxVersion
import org.objectweb.asm.{ClassVisitor, Opcodes}

case class UpgradeClassVersionVisitor(visitor: ClassVisitor, sandBoxVersion: SandBoxVersion)
    extends ClassVisitor(Opcodes.ASM6, visitor) {
  override def visit(version: Int,
                     access: Int,
                     name: String,
                     signature: String,
                     superName: String,
                     interfaces: Array[_root_.java.lang.String]): Unit = {
    val innerVersion = sandBoxVersion.toInnerVersion(version)
    if (visitor != null)
      visitor.visit(innerVersion, access, name, signature, superName, interfaces)
    else super.visit(innerVersion, access, name, signature, superName, interfaces)
  }
}
