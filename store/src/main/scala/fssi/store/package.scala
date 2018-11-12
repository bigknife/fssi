package fssi

import fssi.store.mpt.Hash

package object store {

  object implicits extends Hash.Implicits
}
