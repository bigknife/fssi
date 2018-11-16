package fssi.wallet

import fssi.wallet.controller.LoginController
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.effect.DropShadow
import scalafx.scene.layout.{Region, StackPane}
import scalafx.scene.paint.Color
import scalafx.stage.StageStyle

object WalletMain extends JFXApp {self =>
  lazy val (loginScene, loginController) = {
    val loginResource = getClass.getClassLoader.getResource("ui/login.fxml")
    val loginFxmlLoader = new javafx.fxml.FXMLLoader(loginResource)
    val form: javafx.scene.Parent = loginFxmlLoader.load()

    /*
    val scene = new Scene(400, 300) {
      fill = Color.Transparent
      stylesheets = List("ui/login.css")
      root = new StackPane {
        padding = Insets(10)
        children = List(
          new Region {
            id = "region"
            effect = new DropShadow {
              radius = 10
              fill = Color.Black
            }
          }/*,
          new scalafx.scene.Node(form)*/
        )
      }
    }
    */

    (new Scene(form), loginFxmlLoader.getController[LoginController])
  }

  stage = new PrimaryStage {
    scene = loginScene
  }

  stage.initStyle(StageStyle.Transparent)
  loginController.stage = stage
  loginScene.fill = Color.Transparent
}