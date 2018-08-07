package fssi

import interpreter._
import ast._, uc._

package object edgenode {
  object jsonrpcResource {
    def apply(_setting: Setting.EdgeNodeSetting, p: EdgeNodeProgram[components.Model.Op]): EdgeJsonRpcResource = new EdgeJsonRpcResource {
      val setting = _setting
      val edgeNodeProgram = p
    }
  }
}
