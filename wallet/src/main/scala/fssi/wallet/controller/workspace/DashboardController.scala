package fssi.wallet.controller.workspace
import java.net.URL
import java.util.ResourceBundle

import fssi.wallet.MainFrameFragment

class DashboardController extends javafx.fxml.Initializable{
  override def initialize(location: URL, resources: ResourceBundle): Unit = {

    MainFrameFragment.mainTitle.value = "Dashboard"
  }
}
