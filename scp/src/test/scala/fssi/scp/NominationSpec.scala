package fssi.scp

import fssi.scp.nomination.{V1IsTopSpec, V0IsTopSpec}
import org.scalatest.Suites

class NominationSpec
    extends Suites(
      new V0IsTopSpec(),
      new V1IsTopSpec()
    ) {}
