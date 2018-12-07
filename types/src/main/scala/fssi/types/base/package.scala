package fssi
package types
import fssi.base.BytesValue

package object base {
  object implicits extends BaseTypeImplicits

  type OpaqueBytes = BytesValue[Array[Byte]]
}
