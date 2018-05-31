package fssi.ast.domain.types

import fssi.ast.domain.Statement

case class Proposal(
    statements: Vector[Statement]
)
