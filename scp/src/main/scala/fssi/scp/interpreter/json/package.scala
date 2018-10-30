package fssi
package scp
package interpreter

package object json {

  object implicits
      extends SignatureJsonCodec
      with NodeIDJsonCodec
      with QuorumSetJsonCodec
      with BallotJsonCodec
      with MessageJsonCodec
}
