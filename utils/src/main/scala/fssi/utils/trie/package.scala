package fssi.utils
package object trie {
  type Value = Array[Byte]
  type Key   = Array[Byte]
  type Hash  = Array[Byte]

  type StoreKey = Array[Byte]
  type StoreValue = Array[Byte]

  object Hash {
    def equal(h1: Hash, h2: Hash): Boolean = h1.sameElements(h2)
  }

}
