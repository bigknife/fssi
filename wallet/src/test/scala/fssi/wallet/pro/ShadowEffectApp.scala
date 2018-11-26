package fssi.wallet.pro

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.beans.property.DoubleProperty
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.Slider
import scalafx.scene.effect.DropShadow
import scalafx.scene.layout.{Region, StackPane}
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.stage.StageStyle

object ShadowEffectApp extends JFXApp {
  val p = DoubleProperty(0)

  stage = new PrimaryStage {
    scene = new Scene(400, 300) {

      fill = Color.Transparent
      root = new StackPane {
        style = "-fx-background-color: null;"
        padding = Insets(10)
        children = List(
          new Region {
            style = "-fx-background-radius:20; -fx-background-color: rgba(56, 176, 209, 0.3);"
            effect = new DropShadow(10, Color.Grey)
          }

        )
      }
    }
  }

  stage.initStyle(StageStyle.Transparent)

}
