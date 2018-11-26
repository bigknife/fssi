package fssi.scp

import org.scalatest._

class SCPSpec
    extends Suites(
      new BallotSpec,
      new NominationSpec
    ) {}
