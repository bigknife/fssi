package fssi.wallet.pro.ch01

import scalafx.beans.property._
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.SingleSelectionModel

class AudioConfigModel {
  val minDecibels = 0.0
  val maxDecibels = 160.0
  val selectedDBs = IntegerProperty(0)
  val muting = BooleanProperty(false)
  val genres = ObservableBuffer("Chamber",
    "Country",
    "Cowbell",
    "Metal",
    "Polka",
    "Rock"
  )
  var genreSelectionModel: SingleSelectionModel[String] = _

  def addListenerToGenreSelectionModel(): Unit = {
    genreSelectionModel.selectedIndex.onChange {
      selectedDBs.value = genreSelectionModel.selectedIndex() match {
        case 0 => 80
        case 1 => 100
        case 2 => 150
        case 3 => 140
        case 4 => 120
        case 5 => 130
      }
    }
  }
}
