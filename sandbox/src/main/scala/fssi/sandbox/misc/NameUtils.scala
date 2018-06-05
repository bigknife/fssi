package fssi.sandbox.misc

object NameUtils {
  def classNameToInnerName(name: String): String = name.replaceAll("\\.", "/")

  def descriptorToClassName(desc: String): Option[String] = {
    if(desc.startsWith("L") && desc.endsWith(";")) {
      //this is a class
      Some(desc.substring(1, desc.length - 1).replaceAll("/", "\\."))
    }
    else None
  }

  def innerNameToClassName(innerName: String): String = {
    innerName.replaceAll("/", "\\.")
  }

  def innerNameToDescriptor(innerName: String): String = s"L$innerName;"
}
