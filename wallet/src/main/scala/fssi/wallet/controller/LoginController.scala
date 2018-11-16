package fssi.wallet.controller
import java.net.URL
import java.util.ResourceBundle

import com.jfoenix.controls._

import javafx.scene.input.MouseEvent
import scalafx.application.Platform
import scalafx.stage.Stage

class LoginController extends javafx.fxml.Initializable {

  private var x: Double = 0
  private var y: Double = 0
  var stage: Stage = _

  @javafx.fxml.FXML
  var loginName: JFXTextField = _
  @javafx.fxml.FXML
  var password: JFXPasswordField = _
  @javafx.fxml.FXML
  var loginButton: JFXButton = _
  @javafx.fxml.FXML
  var registerButton: JFXButton = _

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    Platform.runLater {
      loginName.requestFocus()
    }
  }

  @javafx.fxml.FXML
  def mousePressedHandler(me: MouseEvent): Unit = {
    x = me.getScreenX - stage.getX
    y = me.getScreenY - stage.getY
  }

  @javafx.fxml.FXML
  def mouseDraggedHandler(me: MouseEvent): Unit = {
    stage.x = me.getScreenX - x
    stage.y = me.getScreenY - y
  }

  @javafx.fxml.FXML
  def exit(): Unit = {

    Platform.exit()
  }
}
