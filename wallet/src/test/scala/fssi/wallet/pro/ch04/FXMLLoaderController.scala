package fssi.wallet.pro.ch04
import java.net.URL
import java.util.ResourceBundle

import javafx.scene.input.MouseEvent
import scalafx.stage.StageStyle

class FXMLLoaderController extends javafx.fxml.Initializable{
  override def initialize(location: URL, resources: ResourceBundle): Unit = {}

  private var dragAnchorX: Double = 0
  private var dragAnchorY: Double = 0
  private var stage: javafx.stage.Stage = _

  def setStage(stage: javafx.stage.Stage): Unit = {
    this.stage =stage
  }

  @javafx.fxml.FXML
  def mouseDraggedHandler(me: MouseEvent): Unit = {
    stage.setX(me.getScreenX - dragAnchorX)
    stage.setY(me.getScreenY - dragAnchorY)
  }

  @javafx.fxml.FXML
  def mousePressedHandler(me: MouseEvent): Unit = {
    dragAnchorX = me.getScreenX - stage.getX
    dragAnchorY = me.getScreenY - stage.getY
  }

  @javafx.fxml.FXML
  def noFrame(): Unit = {
    println("no frame")
    stage.hide()
    stage.initStyle(StageStyle.Transparent)
    stage.show()
  }
}
