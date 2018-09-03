package fssi
package sandbox
package visitor

import org.objectweb.asm.Opcodes._
import org.objectweb.asm._

case class CheckClassDeterminismVisitor(classLoader: FSSIClassLoader,
                                        visitor: ClassVisitor,
                                        track: scala.collection.mutable.ListBuffer[String],
                                        contractClass: String,
                                        contractMethod: String,
                                        contractMethodParameterTypes: Array[String])
    extends ClassVisitor(Opcodes.ASM6, visitor) {
  import CheckClassDeterminismVisitor._
  import Protocol._
  var currentClassName: String       = _
  var currentClassDescriptor: String = _
  var contractMethodExited: Boolean  = false

  override def visit(version: Int,
                     access: Int,
                     name: String,
                     signature: String,
                     superName: String,
                     interfaces: Array[String]): Unit = {
    currentClassName = Type.getObjectType(name).getClassName
    currentClassDescriptor = Type.getObjectType(name).getDescriptor
    if (visitor != null) visitor.visit(version, access, name, signature, superName, interfaces)
    if (!visitedClasses.contains(currentClassName)) {
      visitedClasses += currentClassName
      forbiddenClasses.find(forbid => forbid.r.pattern.matcher(currentClassDescriptor).matches()) match {
        case Some(_) => track += s"class [$currentClassName] is forbidden"
        case None    =>
      }
      if (track.isEmpty && superName != null) {
        val superClass = Type.getObjectType(superName).getClassName
        if (!visitedClasses.contains(superClass)) {
          visitedClasses += superClass
          classLoader.findClassMethod(superClass, "", Array.empty); ()
        }
      }
    }
  }

  override def visitField(access: Int,
                          name: String,
                          descriptor: String,
                          signature: String,
                          value: Any): FieldVisitor = {
    val fieldVisitor = super.visitField(access, name, descriptor, signature, value)
    if (track.isEmpty) {
      forbiddenClasses.find(forbid => forbid.r.pattern.matcher(descriptor).matches()) match {
        case Some(_) =>
          track += s"class field [$currentClassName.$name] of type [${Type.getType(descriptor).getClassName}] is forbidden"
        case None =>
      }
      if ((ACC_VOLATILE & access) == ACC_VOLATILE)
        track += s"class volatile field [$currentClassName.$name] is forbidden"
    }

    val className = Type.getType(descriptor).getClassName
    if (!visitedClasses.contains(className)) classLoader.loadClass(className)
    fieldVisitor
  }

  override def visitMethod(access: Int,
                           name: String,
                           descriptor: String,
                           signature: String,
                           exceptions: Array[String]): MethodVisitor = {
    val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
    if ((access & ACC_SYNCHRONIZED) == ACC_SYNCHRONIZED)
      track += s"class method [$currentClassName.$name] should not with access 'synchronized'"

    if ((access & ACC_NATIVE) == ACC_NATIVE && !currentClassName.startsWith("java.lang.Object"))
      track += s"class method [$currentClassName.$name] should not with access 'native'"

    val argTypes = Type.getMethodType(descriptor).getArgumentTypes.map(_.getClassName)
    if (contractClass == currentClassName && contractMethod == name && (argTypes sameElements contractMethodParameterTypes)) {
      contractMethodExited = true
    }
    CheckMethodDeterminismVisitor(methodVisitor,
                                  name,
                                  currentClassName,
                                  currentClassDescriptor,
                                  track)
  }

  override def visitEnd(): Unit = {
    super.visitEnd()
    if (currentClassName == contractClass && !contractMethodExited) {
      track += s"contract method [$contractClass#$contractMethod(${contractMethodParameterTypes.mkString(",")})] not found"
    }
  }
}

object CheckClassDeterminismVisitor {

  private[CheckClassDeterminismVisitor] lazy val visitedClasses =
    scala.collection.mutable.ListBuffer.empty[String]
}
