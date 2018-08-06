package fssi

package object types {
  type TokenUnit = Token.Unit

  /** JsonMessageHandler */
  type JsonMessageHandler = JsonMessage => Unit

  object syntax extends BytesValue.Syntax
}
