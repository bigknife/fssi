package fssi.wallet

import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.stage.Stage

trait RouteSupport {
  def changeScene(scene: Scene, stage: Stage): Unit = {
    stage.scene = scene
    scene.fill = Color.Transparent
    stage.sizeToScene()
    stage.centerOnScreen()
  }

  def gotoLogin(stage: Stage): Unit
  def gotoProfile(stage: Stage): Unit
  def gotoDashboard(stage: Stage): Unit
}
