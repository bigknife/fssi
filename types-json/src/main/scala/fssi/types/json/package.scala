package fssi
package types

package object json {

  /** the io.circe json codecs.
    * import io.circe._,io.circe.syntax._
    * import fssi.types.json.implicits._
    */
  object implicits extends AllTypesJsonCodec
}
