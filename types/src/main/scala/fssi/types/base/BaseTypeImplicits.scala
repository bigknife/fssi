package fssi
package types
package base

trait BaseTypeImplicits
    extends BytesValue.Implicits
    with Hash.Implicits
    with Base58Check.Implicits
    with Signature.Implicits
    with UniqueName.Implicits
