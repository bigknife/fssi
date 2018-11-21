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
import scalafx.scene.text.Font
import scalafx.stage.StageStyle

object WalletMain extends JFXApp with LoginFragment  with MainFrameFragment {
  self =>

  fssi.utils.crypto.registerBC()
  //load font
  Font.loadFont(getClass.getClassLoader.getResource("ui/font/Metropolis-Regular.otf").toExternalForm, 10)

  stage = new PrimaryStage
  stage.setMinHeight(768)
  stage.setMinWidth(1024)
  stage.setTitle("FSSI Wallet - A FSSI Block Chain Client")
  //stage.initStyle(StageStyle.Transparent)

  //gotoLogin(stage)
  gotoMainFrame(stage)
}
