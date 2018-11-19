package fssi.wallet

import javafx.fxml.FXMLLoader
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.stage.Stage

trait RouteSupport {

  def fxmlLoaderOf(xml: String): FXMLLoader = new FXMLLoader(getClass.getClassLoader.getResource(xml))

  def changeScene(scene: Scene, stage: Stage): Unit = {
    stage.scene = scene
    scene.fill = Color.Transparent
    stage.sizeToScene()
    stage.centerOnScreen()
  }

  def gotoLogin(stage: Stage): Unit
  //def gotoProfile(stage: Stage): Unit
  //def gotoDashboard(stage: Stage): Unit
  def gotoMainFrame(stage: Stage): Unit
}
