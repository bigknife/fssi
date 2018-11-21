package fssi.wallet
import fssi.wallet.controller.ProfileController
import javafx.fxml.FXMLLoader
import scalafx.scene.Scene
import scalafx.stage.Stage
import scalafx.Includes._

trait ProfileFragment extends RouteSupport{

  private var profileScene: Scene = _
  private var profileController: ProfileController = _

  def gotoProfile(stage: Stage): Unit = {
    if (profileScene == null) {
      val fxmlResource = getClass.getClassLoader.getResource("ui/profile.fxml")
      val fxmlLoader = new FXMLLoader(fxmlResource)
      val parent = fxmlLoader.load[javafx.scene.Parent]
      profileScene = new Scene(parent)
      profileController = fxmlLoader.getController[ProfileController]
      profileController.stage = stage
    }

    changeScene(profileScene, stage)
  }
}
