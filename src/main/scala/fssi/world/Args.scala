package fssi.world

sealed trait Args

object Args {
  case class EmptyArgs() extends Args

  def default: Args = EmptyArgs()

  trait Implicits {

    implicit object parser extends _root_.scopt.OptionParser[Args]("fssi") {

      head("fssi", "0.0.1")
    }
  }

  object implicits extends Implicits
}
