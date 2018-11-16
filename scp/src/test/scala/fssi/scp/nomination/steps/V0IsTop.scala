package fssi.scp.nomination.steps

trait V0IsTop extends StepSpec{
  def v0IsTop(): Unit = {
    app.liftNodePriority(node0)
    info("vo is top")
  }
}
