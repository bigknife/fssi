package fssi.scp.nomination
import fssi.scp.nomination.steps.{OthersNominateY, V0IsTop}
import fssi.scp.types.{Ballot, Envelope, ValueSet}
import fssi.scp.types.Message.Nomination
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.scalatest.Matchers._

class V0XOthersYSuite extends FunSuite with BeforeAndAfterEach with V0IsTop with OthersNominateY {
  override def suiteName: String = "self nominates 'x', others nominate y -> prepare y"

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    super.afterEach()
  }

  test("others only vote for y") {
    selfNominateXAndOthersNominateY()

    val nom1: Envelope[Nomination] =
      app.makeNomination(node1, keyOfNode1, votesY.unsafe(), myAccepted.unsafe())
    val nom2: Envelope[Nomination] =
      app.makeNomination(node2, keyOfNode2, votesY.unsafe(), myAccepted.unsafe())
    val nom3: Envelope[Nomination] =
      app.makeNomination(node3, keyOfNode3, votesY.unsafe(), myAccepted.unsafe())
    val nom4: Envelope[Nomination] =
      app.makeNomination(node4, keyOfNode4, votesY.unsafe(), myAccepted.unsafe())

    // nothing happened
    app.onEnvelope(nom1)
    app.onEnvelope(nom2)
    app.onEnvelope(nom3)
    app.numberOfEnvelopes shouldBe 1

    // 'y' is myAccepted (quorum)
    app.onEnvelope(nom4)
    app.numberOfEnvelopes shouldBe 2
    myVotes := ValueSet(xValue, yValue)
    app.hasNominated(myVotes.unsafe(), acceptedY.unsafe())
  }

  test("others myAccepted y") {
    selfNominateXAndOthersNominateY()

    val acc1: Envelope[Nomination] =
      app.makeNomination(node1, keyOfNode1, votesY.unsafe(), acceptedY.unsafe())
    val acc2: Envelope[Nomination] =
      app.makeNomination(node2, keyOfNode2, votesY.unsafe(), acceptedY.unsafe())
    val acc3: Envelope[Nomination] =
      app.makeNomination(node3, keyOfNode3, votesY.unsafe(), acceptedY.unsafe())
    val acc4: Envelope[Nomination] =
      app.makeNomination(node4, keyOfNode4, votesY.unsafe(), acceptedY.unsafe())

    app.onEnvelope(acc1)
    app.numberOfEnvelopes shouldBe 1

    // this causes 'y' to be accepted (v-blocking)
    app.onEnvelope(acc2)
    app.numberOfEnvelopes shouldBe 2
    app.hasNominated(ValueSet(xValue, yValue), acceptedY.unsafe())

    app.forecastNomination(ValueSet(yValue), Some(yValue))

    // this causes the node to send a prepare message (quorum)
    app.onEnvelope(acc3)
    app.numberOfEnvelopes shouldBe 3
    app.hasPrepared(Ballot(1, yValue))

    app.onEnvelope(acc4)
    app.numberOfEnvelopes shouldBe 3
  }
}
