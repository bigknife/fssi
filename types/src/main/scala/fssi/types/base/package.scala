package fssi
package types

package object base {
  type Base58Checksum = Base58Check.Checksum
  object implicits extends BaseTypeImplicits
}
