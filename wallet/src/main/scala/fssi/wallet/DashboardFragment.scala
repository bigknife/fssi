package fssi.wallet

import fssi.wallet.controller.DashboardController
import javafx.fxml.FXMLLoader
import scalafx.scene.Scene
import scalafx.stage.Stage
import scalafx.Includes._

trait DashboardFragment extends RouteSupport {
  var dashboardScene: Scene = _
  var dashboardController: DashboardController = _

  def gotoDashboard(stage: Stage): Unit = {

    if (dashboardScene == null) {
      val resource = getClass.getClassLoader.getResource("ui/dashboard.fxml")
      val fxmlLoader = new FXMLLoader(resource)
      val parent: javafx.scene.Parent = fxmlLoader.load()
      dashboardScene = new Scene(parent)
      dashboardController = fxmlLoader.getController[DashboardController]
      dashboardController.stage = stage
    }

    changeScene(dashboardScene, stage)

  }

}
