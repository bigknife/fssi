package fssi
package sandbox
package visitor

trait VisitorChecker {

  import fssi.sandbox.types.Protocol._

  protected lazy val visitedClass =scala.collection.mutable.Set.empty[String]

  def isClassAlreadyVisited(className:String):Boolean = {
    visitedClass.contains(className)
  }

  def isDescriptorIgnored(descriptor:String):Boolean = {
    ignoreDescriptors.exists(ignoreDescriptor => ignoreDescriptor.r.pattern.matcher(descriptor).matches())
  }

  def isDescriptorForbidden(descriptor:String):Boolean = {
    forbiddenDescriptor.exists(forbiddenDescriptor => forbiddenDescriptor.r.pattern.matcher(descriptor).matches())
  }

  def isPackageForbidden(packageName:String):Boolean  = {
    forbiddenPackage.exists(packName =>packageName.startsWith(packName))
  }


}
