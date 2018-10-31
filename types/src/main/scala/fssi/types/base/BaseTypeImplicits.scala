package fssi
package types
package base
import fssi.base.{Base58Check, BytesValue}

trait BaseTypeImplicits
    extends BytesValue.Implicits
    with Hash.Implicits
    with Base58Check.Implicits
    with Signature.Implicits
    with UniqueName.Implicits
    with WorldState.Implicits
