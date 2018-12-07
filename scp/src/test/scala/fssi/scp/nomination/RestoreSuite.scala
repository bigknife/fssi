package fssi.scp.nomination
import fssi.scp.interpreter.store.{BallotStatus, NominationStatus}
import fssi.scp.nomination.steps.{OthersNominateWhatV0Nominates, RestoreNomination, V0IsTop}
import fssi.scp.types.Ballot
import fssi.scp.types.Message.Prepare
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.scalatest.Matchers._

class RestoreSuite
    extends FunSuite
    with BeforeAndAfterEach
    with V0IsTop
    with OthersNominateWhatV0Nominates
    with RestoreNomination {
  override def suiteName: String = "restored state"

  override def beforeEach(): Unit = {
    super.beforeEach()

    v0IsTop()
    othersNominateWhatV0Nominate()
  }

  override def afterEach(): Unit = {

    super.afterEach()
  }

  test("ballot not started") {
    NominationStatus.clearInstance(slot1)
    BallotStatus.cleanInstance(slot1)
    restoreNomination()

    app2.numberOfEnvelopes shouldBe 2
    app2.shouldHavePrepared(Ballot(1, xValue))
  }

  test("ballot started (on value z)") {
    app2.setStatusFromMessage(Prepare(Ballot(1, zValue)))
    NominationStatus.clearInstance(slot1)
    restoreNomination()

    // nomination didn't do anything (already working on z)
    app2.numberOfEnvelopes shouldBe 1
  }
}
