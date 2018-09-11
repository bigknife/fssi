package fssi
package sandbox
package visitor

import fssi.sandbox.types.SandBoxVersion
import org.objectweb.asm.{ClassVisitor, Opcodes}

case class DegradeClassVersionVisitor(visitor: ClassVisitor, sandBoxVersion: SandBoxVersion)
    extends ClassVisitor(Opcodes.ASM6, visitor) {

  override def visit(version: Int,
                     access: Int,
                     name: String,
                     signature: String,
                     superName: String,
                     interfaces: Array[String]): Unit = {
    val outerVersion = sandBoxVersion.toOuterVersion(version)
    if (visitor != null)
      visitor.visit(outerVersion, access, name, signature, superName, interfaces)
    else
      super.visit(outerVersion, access, name, signature, superName, interfaces)
  }
}
