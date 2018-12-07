package fssi
import fssi.edgenode.handler.{ApplicationMessageHandler, ClientMessageHandler}

package object edgenode {
  object applicationMessageHandler extends ApplicationMessageHandler
  object clientMessageHandler      extends ClientMessageHandler
}
