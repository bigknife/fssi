package fssi
package sandbox
package visitor

import org.objectweb.asm.{ClassVisitor, Opcodes}

case class DegradeClassVersionVisitor(visitor: ClassVisitor)
    extends ClassVisitor(Opcodes.ASM6, visitor) {

  override def visit(version: Int,
                     access: Int,
                     name: String,
                     signature: String,
                     superName: String,
                     interfaces: Array[String]): Unit = {
    val sandboxVersion = SandBoxVersion(version)
    if (sandboxVersion.isVersionValid) {
      val outerVersion = sandboxVersion.toOuterVersion
      if (visitor != null)
        visitor.visit(outerVersion.value, access, name, signature, superName, interfaces)
      else
        super.visit(outerVersion.value, access, name, signature, superName, interfaces)
    } else throw new IllegalArgumentException(s"class file version $version is not support")
  }
}
