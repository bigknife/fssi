package fssi.types
package biz

case class ChainConfiguration(
    host: String,
    port: Int,
    seeds: Vector[Node.Addr],
    account: Account
)
