package fssi.wallet.controller
import java.net.URL
import java.util.ResourceBundle

import javafx.scene.input.MouseEvent
import scalafx.stage.Stage
import scalafx.Includes._

class DashboardController extends javafx.fxml.Initializable {
  var stage: Stage = _
  val dw = new DragWindow

  override def initialize(location: URL, resources: ResourceBundle): Unit = {

  }

  @javafx.fxml.FXML
  def mousePressedHandler(me: MouseEvent): Unit = {
    dw.mousePressed(me, stage)
  }

  @javafx.fxml.FXML
  def mouseDraggedHandler(me: MouseEvent): Unit = {
    dw.mouseDragged(me, stage)
  }
}
