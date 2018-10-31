package fssi.scp.nomination.steps

trait OthersNominateWhatV0Nominates extends StepSpec {
  def othersNominateWhatV0Nominate(): Unit = {
    require(app.nominate(xValue))
  }
}
