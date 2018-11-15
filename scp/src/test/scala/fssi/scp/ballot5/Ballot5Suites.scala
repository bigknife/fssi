package fssi.scp.ballot5
import fssi.scp.ballot5.normal1x.NormalRound1XSuites
import fssi.scp.ballot5.pristine.StartFromPristineSuites
import fssi.scp.ballot5.start1x.Start1XSuites
import fssi.scp.ballot5.start1y.Start1YSuites
import org.scalatest.Suites

class Ballot5Suites
    extends Suites(
      new BumpStateXSuite(),
      new Start1XSuites(),
      new Start1YSuites(),
      new StartFromPristineSuites(),
      new NormalRound1XSuites(),
      new RangeCheckSuite(),
      new NetworkSuite(),
      new RestoreSuite
    ) {}
