package fssi.wallet.pro.ch02

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.stage.StageStyle

object TweStageMain extends JFXApp {self =>
  val stage1: PrimaryStage = new PrimaryStage {
    scene = new Scene {
      content = List(
        new Button {
          text = "change scene to 2"
          prefWidth = 150
          prefHeight = 30
          onMouseClicked = {_ =>
            println("hello")
             self.stage = stage2
          }
        }
      )
    }
  }

  lazy val stage2: PrimaryStage = new PrimaryStage {
    scene = new Scene {
      content = List(
        new Button {
          text = "change scene to 1"
          prefWidth = 150
          prefHeight = 30
          onMouseClicked = e => self.stage = stage1
        }
      )
    }
  }

  stage = stage1

}
