package fssi.scp

import fssi.scp.nomination.{V1IsTopSuite, V0IsTopSuite}
import org.scalatest.Suites

class NominationSpec
    extends Suites(
      new V0IsTopSuite(),
      new V1IsTopSuite()
    ) {}
