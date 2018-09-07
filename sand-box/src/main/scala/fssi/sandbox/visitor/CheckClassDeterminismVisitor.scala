package fssi
package sandbox
package visitor

import fssi.sandbox.loader.FSSIClassLoader
import org.objectweb.asm.Opcodes._
import org.objectweb.asm._

case class CheckClassDeterminismVisitor(classLoader: FSSIClassLoader,
                                        visitor: ClassVisitor,
                                        track: scala.collection.mutable.ListBuffer[String],
                                        className: String,
                                        methodName: String,
                                        methodParameterTypes: Array[String],
                                        needCheckMethod: Boolean)
    extends ClassVisitor(Opcodes.ASM6, visitor) {
  private[CheckClassDeterminismVisitor] lazy val visitedClasses =
    scala.collection.mutable.ListBuffer.empty[String]

  import fssi.sandbox.types.Protocol._
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
    else super.visit(version, access, name, signature, superName, interfaces)
    if (!ignoreClasses.contains(currentClassDescriptor)) {
      if (!visitedClasses.contains(currentClassName)) {
        visitedClasses += currentClassName
        forbiddenClasses.find(classDescriptor =>
          classDescriptor.r.pattern.matcher(currentClassDescriptor).matches()) match {
          case Some(_) => track += s"class [$currentClassName] is forbidden"
          case None    =>
        }
        forbiddenPackage.find(packName => currentClassName.startsWith(packName)) match {
          case Some(_) => track += s"class [$currentClassName] is forbidden"
          case None    =>
        }
        if (track.isEmpty && superName != null) {
          val superClass = Type.getObjectType(superName).getClassName
          if (!visitedClasses.contains(superClass)) {
            visitedClasses += superClass
            classLoader.findClass(superClass, "", Array.empty); ()
          }
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
    if (!visitedClasses.contains(className)) classLoader.findClass(className, "", Array.empty)
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
    if (needCheckMethod && className == currentClassName && methodName == name && (argTypes sameElements methodParameterTypes)) {
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
    if (needCheckMethod && currentClassName == className && !contractMethodExited) {
      track += s"contract method [$className#$methodName(${methodParameterTypes.mkString(",")})] not found"
    }
  }
}
