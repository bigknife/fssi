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

object WalletMain extends JFXApp with LoginFragment with ProfileFragment with DashboardFragment {
  self =>

  stage = new PrimaryStage
  stage.initStyle(StageStyle.Transparent)

  gotoLogin(stage)
}
