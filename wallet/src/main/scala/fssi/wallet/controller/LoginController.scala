package fssi.wallet.controller
import java.net.URL
import java.util.ResourceBundle

import com.jfoenix.controls._
import fssi.wallet.WalletMain
import javafx.scene.input.MouseEvent
import scalafx.application.Platform
import scalafx.stage.Stage
import scalafx.Includes._

class LoginController extends javafx.fxml.Initializable {
  var stage: Stage = _
  val dw: DragWindow = new DragWindow()


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
    dw.mousePressed(me, stage)
  }

  @javafx.fxml.FXML
  def mouseDraggedHandler(me: MouseEvent): Unit = {
    dw.mouseDragged(me, stage)
  }

  @javafx.fxml.FXML
  def exit(): Unit = {
    Platform.exit()
  }

  @javafx.fxml.FXML
  def login(): Unit = {
    WalletMain.gotoProfile(stage)
  }

  @javafx.fxml.FXML
  def register(): Unit = {
    WalletMain.gotoDashboard(stage)
  }
}
