package fssi.scp.ballot3.steps

import org.scalatest.GivenWhenThen
import org.scalatest.Matchers._

trait Ballot3 extends TestBed with GivenWhenThen {
  def prepareBallot3TestBed(): Unit = {
    app.bumpState(aValue) shouldBe true
    app.numberOfEnvelopes shouldBe 1
    app.shouldNotHaveBallotTimer()
  }
}
