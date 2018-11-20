package fssi.wallet
import fssi.wallet.controller.{MainFrameController, WorkspaceLoader}
import scalafx.scene.Scene
import scalafx.stage.Stage
import scalafx.Includes._

trait MainFrameFragment extends RouteSupport {
  private var mainFrameScene: Scene = _
  private var mainFrameController: MainFrameController = _

  private def init(stage: Stage): Unit = {
    if (mainFrameScene == null) {
      val fxmlLoader = fxmlLoaderOf("ui/MainFrame.fxml")
      val parent: javafx.scene.Parent = fxmlLoader.load()
      mainFrameScene = new Scene(parent)
      mainFrameController = fxmlLoader.getController[MainFrameController]
      mainFrameController.stage = stage
    }
  }

  override def gotoMainFrame(stage: Stage): Unit = {
    init(stage)
    changeScene(mainFrameScene, stage)

    // workspace to default view (may be dashboard)
    WorkspaceLoader << "ui/workspace/dashboard.fxml"
  }
}
