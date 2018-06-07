package fssi.sandbox.misc

import java.util.regex.Pattern

import scala.annotation.tailrec

object ClassNameUtil {
  val ArrayRefTypePatternInternal: Pattern = "((\\[+)L[^;]+;)".r.pattern
  val JavaLangPatternInternal: Pattern     = "^java/lang/(.*)".r.pattern

  val SandboxPrefixInternal           = "sandbox/"
  val SandboxPatternInternal: Pattern = s"^$SandboxPrefixInternal(.*)".r.pattern

  def sandboxInternalTypeName(internallClassName: String): String = {
    if (classShouldBeSandboxedInternal(internallClassName)) {
      val arrayMatch = ArrayRefTypePatternInternal.matcher(internallClassName)
      if (arrayMatch.find()) {
        val indirection = arrayMatch.group(2)
        s"$indirection$SandboxPrefixInternal${internallClassName.substring(indirection.length)}"
      } else s"$SandboxPrefixInternal$internallClassName"
    } else internallClassName
  }

  def sandboxQualifiedTypedName(qualifiedName: String): String = {
    val internal          = convertQualifiedClassNameToInternalForm(qualifiedName)
    val sandboxedInternal = sandboxInternalTypeName(internal)
    if(internal == sandboxedInternal) qualifiedName
    else convertInternalFromToQualifiedClassName(sandboxedInternal)
  }

  def convertQualifiedClassNameToInternalForm(qualifiedName: String): String =
    qualifiedName.replaceAll("\\.", "/")

  def convertInternalFromToQualifiedClassName(internalClassName: String): String =
    internalClassName.replaceAll("/", "\\.")

  @tailrec
  def classShouldBeSandboxedInternal(className: String): Boolean =
    if (ArrayRefTypePatternInternal.asPredicate().test(className))
      classShouldBeSandboxedInternal(className.substring(2, className.length - 1))
    else if (JavaLangPatternInternal.asPredicate().test(className)) false
    else !SandboxPatternInternal.asPredicate().test(className)
}
