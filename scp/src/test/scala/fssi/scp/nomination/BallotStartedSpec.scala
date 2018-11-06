package fssi.scp.nomination
import fssi.scp.nomination.steps.{OthersNominateWhatV0Nominates, RestoreNomination, V0IsTop}
import org.scalatest.FunSuite

class BallotStartedSpec
    extends FunSuite
    with V0IsTop
    with OthersNominateWhatV0Nominates
    with RestoreNomination {


}
