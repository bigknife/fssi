package fssi
package sandbox
package visitor
package clazz

import fssi.sandbox.loader.FSSIClassLoader
import fssi.sandbox.visitor.method.CheckMethodDeterminismVisitor
import org.objectweb.asm.Opcodes._
import org.objectweb.asm._

case class CheckClassDeterminismVisitor(classLoader: FSSIClassLoader,
                                        visitor: ClassVisitor,
                                        track: scala.collection.mutable.ListBuffer[String],
                                        className: String,
                                        methodName: String,
                                        methodParameterTypes: Array[String],
                                        needCheckMethod: Boolean)
    extends ClassVisitor(ASM6, visitor)
    with VisitorChecker {

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

    if (!isClassAlreadyVisited(currentClassName)) {
      visitedClass.add(currentClassName)
      if (!isDescriptorIgnored(currentClassDescriptor)) {
        if (isDescriptorForbidden(currentClassDescriptor)) {
          track += s"class [$currentClassName] is forbidden"
        }
        if (isPackageForbidden(currentClassName)) {
          track += s"class [$currentClassName] is forbidden"
        }
      }
      if (track.isEmpty && superName != null) {
        val superClassName       = Type.getObjectType(superName).getClassName
        val superClassDescriptor = Type.getObjectType(superName).getDescriptor
        if (!isClassAlreadyVisited(superClassName))
          if (!isDescriptorIgnored(superClassDescriptor)) {
            classLoader.findClass(superClassName, "", Array.empty); ()
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
//      val className = Type.getType(descriptor).getClassName
//      if(!isClassAlreadyVisited(className)){
//        classLoader.findClass(className, "", Array.empty)
//      }
      if (isDescriptorForbidden(descriptor)) {
        track += s"class field [$currentClassName.$name] of type [${Type.getType(descriptor).getClassName}] is forbidden"
      }
      if ((ACC_VOLATILE & access) == ACC_VOLATILE)
        track += s"class volatile field [$currentClassName.$name] is forbidden"
    }
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
