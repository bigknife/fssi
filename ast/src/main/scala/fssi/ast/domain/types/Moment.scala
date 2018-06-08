package fssi.ast.domain.types

import fssi.contract.States

/**
  * Moment is: the chaning of the world state. it's a triple: (initStates, transaction, newStates)
  */
case class Moment(
    oldStates: States,
    transaction: Transaction,
    newStates: States,
    oldStatesHash: BytesValue,
    newStatesHash: BytesValue
)
