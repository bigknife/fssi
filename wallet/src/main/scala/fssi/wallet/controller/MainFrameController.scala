package fssi.wallet.controller
import java.net.URL
import java.util.ResourceBundle

import fssi.wallet.MainFrameFragment
import javafx.fxml.FXMLLoader
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import scalafx.stage.{Screen, Stage}
import scalafx.Includes._
import scalafx.application.Platform

class MainFrameController extends javafx.fxml.Initializable{
  val dw = new DragWindow

  var stage: Stage = _

  @javafx.fxml.FXML
  var workspace: BorderPane = _

  @javafx.fxml.FXML
  var cMainTitle: Label = _

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    WorkspaceLoader.listen {(oldValue, newValue) =>
      println(s"workspace changing view: $oldValue => $newValue")
      loadWorkspace(newValue)
    }

    cMainTitle.textProperty() <== MainFrameFragment.mainTitle
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
  def gotoDashboard(): Unit = WorkspaceLoader << "ui/workspace/dashboard.fxml"

  @javafx.fxml.FXML
  def gotoCreateAccount(): Unit = WorkspaceLoader << "ui/workspace/create_account.fxml"


  @javafx.fxml.FXML
  def changeSize(): Unit = {
    val screen = Screen.primary
    val bounds = screen.getBounds

    stage.setX(bounds.getMinX)
    stage.setY(bounds.getMinY)
    stage.setWidth(bounds.getWidth)
    stage.setHeight(bounds.getHeight)
  }

  private def loadWorkspace(ui: String): Unit = {
    // todo: cache ui, and invalidate when re-login
    val loader = new FXMLLoader(getClass.getClassLoader.getResource(ui))
    val node: javafx.scene.Node = loader.load()
    workspace.setCenter(node)
    // maybe add some transition animation
  }
}
