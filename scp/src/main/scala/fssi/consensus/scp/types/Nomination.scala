package fssi.consensus.scp.types

/**
  * SCP Nomination Protocol State Holder
  * @param node current node to run nomination protocol
  * @param voted the values that current node has voted to `nominate x`
  * @param accepted the values that current node has voted to `accept(nominate x)`
  * @param candidates candidate values, ie. all quorum including current node has started `accepted(nominate x)`
  * @param messages the latest messages from each node, from these messages, we can recompute `voted`,`accepted`,`candidates`
  * @tparam A the type of the values
  */
case class Nomination[A](
    node: Node,
    voted: Set[A],
    accepted: Set[A],
    candidates: Set[A],
    messages: Set[Nomination.Message[A]]
)

object Nomination {

  /**
    * Nomination message
    * @tparam A the type of the values
    */
  sealed trait Message[A] {
    def node: Node
    def slotIndex: Int
    def voted: Set[A]
    def accepted: Set[A]
    def quorumSlices: Set[QuorumSlice]
  }

  /**
    * Nominate x
    * @param node node that message sent from
    * @param slotIndex the slot index to vote for
    * @param voted voted values from node state
    * @param accepted accepted values from node state
    * @param quorumSlices quorum slices of the node
    * @tparam A the type of the values
    */
  case class Nominate[A](node: Node,
                         slotIndex: Int,
                         voted: Set[A],
                         accepted: Set[A],
                         quorumSlices: Set[QuorumSlice])
      extends Message[A]
  case class ConfirmNomination[A](node: Node,
                                  slotIndex: Int,
                                  voted: Set[A],
                                  accepted: Set[A],
                                  quorumSlices: Set[QuorumSlice])
      extends Message[A]
}
