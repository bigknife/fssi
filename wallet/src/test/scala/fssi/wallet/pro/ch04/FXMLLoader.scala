package fssi.wallet.pro.ch04

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.Includes._

object FXMLLoader extends JFXApp {
  val resource = getClass.getClassLoader.getResource("StageCoach.fxml")
  if(resource == null) throw new RuntimeException("fxml not found")
  val fxmlLoader = new javafx.fxml.FXMLLoader(resource)

  val root:javafx.scene.Parent = fxmlLoader.load()
  val controller = fxmlLoader.getController[FXMLLoaderController]

  stage = new PrimaryStage {
    title = "load fxml"
    scene = new Scene(root)
  }

  controller.setStage(stage)
}
