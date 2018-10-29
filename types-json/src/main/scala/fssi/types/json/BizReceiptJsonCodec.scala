package fssi
package types
package json

import io.circe._
import types.json.implicits._
import io.circe.syntax._
import io.circe.generic.auto._

trait BizReceiptJsonCodec {

  implicit val stackTraceElementEncoder: Encoder[StackTraceElement] = (st: StackTraceElement) =>
    Json.obj(
      "className"  -> Json.fromString(st.getClassName),
      "methodName" -> Json.fromString(st.getMethodName),
      "lineNumber" -> Json.fromInt(st.getLineNumber),
      "fileName"   -> Json.fromString(st.getFileName)
  )

  implicit val stackTraceElementDecoder: Decoder[StackTraceElement] = (hCursor: HCursor) => {
    for {
      className  <- hCursor.get[String]("className")
      methodName <- hCursor.get[String]("methodName")
      lineNumber <- hCursor.get[Int]("lineNumber")
      fileName   <- hCursor.get[String]("fileName")
    } yield new StackTraceElement(className, methodName, fileName, lineNumber)
  }
}
