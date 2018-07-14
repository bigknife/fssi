package fssi.ast.domain.types

case class TimeCapsule(
    height: BigInt,
    moments: Vector[Moment],
    hash: Hash,
    previousHash: Hash
)