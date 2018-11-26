package fssi.wallet

import fssi.wallet.controller.LoginController
import scalafx.scene.Scene
import scalafx.Includes._
import scalafx.stage.Stage

trait LoginFragment extends RouteSupport {
  private var loginScene: Scene                = _
  private var loginController: LoginController = _

  /** jump to login page */
  def gotoLogin(stage: Stage): Unit = {
    if (loginScene == null) {
      val loginResource             = getClass.getClassLoader.getResource("ui/login.fxml")
      val loginFxmlLoader           = new javafx.fxml.FXMLLoader(loginResource)
      val form: javafx.scene.Parent = loginFxmlLoader.load()

      loginScene = new Scene(form)
      loginController = loginFxmlLoader.getController[LoginController]

      loginController.stage = stage
    }

    changeScene(loginScene, stage)

  }
}
