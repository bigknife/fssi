package fssi.scp.nomination.steps
import fssi.scp.types.Message.Nomination
import fssi.scp.types.{Envelope, ValueSet}

trait V1IsTop extends StepSpec {

  val votesX: ValueSet  = ValueSet(xValue)
  val votesY: ValueSet  = ValueSet(yValue)
  val votesZ: ValueSet  = ValueSet(zValue)
  val votesXY: ValueSet = ValueSet(xValue, yValue)
  val votesYZ: ValueSet = ValueSet(yValue, zValue)
  val votesXZ: ValueSet = ValueSet(xValue, zValue)
  val emptyV: ValueSet  = ValueSet.empty

  val nom1: Envelope[Nomination] = app.makeNomination(node1, keyOfNode1, votesXY, emptyV)
  val nom2: Envelope[Nomination] = app.makeNomination(node2, keyOfNode2, votesXZ, emptyV)

  def v1IsTop(): Unit = {
    app.liftNodePriority(node1)

    app.hashOfValue(
      (xValue, 1),
      (yValue, 2),
      (zValue, 3)
    )
  }
}
