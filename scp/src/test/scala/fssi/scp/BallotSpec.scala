package fssi.scp
import fssi.scp.ballot3.Ballot3Suites
import fssi.scp.ballot5.Ballot5Suites
import org.scalatest.Suites

class BallotSpec
    extends Suites(
      new Ballot5Suites,
      new Ballot3Suites
    ) {}
