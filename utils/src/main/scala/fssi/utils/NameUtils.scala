package fssi.utils

/**
  * Created on 2018/8/14
  */
object NameUtils {

  def classNameToInnerName(name: String): String = name.replaceAll("\\.", "/")

  def descriptorToClassName(desc: String): Option[String] =
    if (desc.startsWith("L") && desc.endsWith(";"))
      Some(desc.substring(1, desc.length - 1) replaceAll ("/", "\\."))
    else None

  def innerNameToClassName(innerName: String): String = innerName.replaceAll("/", "\\.")

  def innerNameToDescriptor(innerName: String): String = s"L$innerName"
}
