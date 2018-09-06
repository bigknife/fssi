package fssi
package sandbox
package types

case class Method(
    alias: String,
    className: String,
    methodName: String,
    parameterTypes: Array[SParameterType]
) {
  override def toString: String =
    s"$alias = $className#$methodName(${parameterTypes.map(_.`type`.getSimpleName).mkString(",")})"
}
