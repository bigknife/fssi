package fssi.wallet.controller.workspace
import java.net.URL
import java.nio.charset.Charset
import java.util.ResourceBundle

import javafx.fxml._
import javafx.scene.canvas.Canvas
import javafx.scene.input.{MouseEvent => JME}
import javafx.scene.layout.HBox
import scalafx.Includes._
import scalafx.beans.property.StringProperty
import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import fssi.interpreter.{Setting, runner}
import fssi.base._
import fssi.types.base.RandomSeed
import fssi.types.biz.Account
import fssi.wallet.{MainFrameFragment, Program, WorkingThreadPool}
import javafx.scene.control.{Label, TextArea, TextField}
import scalafx.application.Platform
import scalafx.stage.FileChooser
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import fssi.types.json.implicits._
import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType
import fssi.types.implicits._

class CreateAccountController extends javafx.fxml.Initializable {
  @FXML
  var cCanvas: Canvas = _
  @FXML
  var cCanvasParent: HBox = _
  @FXML
  var cRandomText: Label = _
  @FXML
  var cAccountIv: TextField = _
  @FXML
  var cAccountPrv: TextField = _
  @FXML
  var cAccountPub: TextField = _
  @FXML
  var cAccountId: TextField = _
  @FXML
  var cAccountSec: TextField = _

  var gc: GraphicsContext                = _
  val currentAccount: Var[Account]       = Var.empty
  val currentSec: Var[Account.SecretKey] = Var.empty

  private val randomBytes  = Array.fill(1024)(Byte.MinValue)
  private val randomString = StringProperty("")

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    //val heightProperty = DoubleProperty(0)
    //val widthProperty = DoubleProperty(0)

    cCanvas.heightProperty() <== cCanvasParent.heightProperty() - 5
    cCanvas.widthProperty() <== cCanvasParent.widthProperty() - 5

    gc = cCanvas.getGraphicsContext2D

    gc.stroke = Color.web("#F5A623")
    gc.fill = Color.rgb(151, 151, 151)
    gc.lineWidth = 1

    cRandomText.textProperty() <== randomString
    //cSavePath.textProperty() <==> savePathProperty

    MainFrameFragment.mainTitle.value = "Create Account"
  }

  @FXML
  def mousePressed(e: JME): Unit = {
    gc.beginPath()
    gc.lineTo(e.getX, e.getY)
    gc.stroke()
  }

  @FXML
  def mouseDragged(e: JME): Unit = {
    //gc.fillRect(e.getX, e.getY, 1, 1)
    gc.lineTo(e.getX, e.getY)
    gc.stroke()

    WorkingThreadPool.delay {
      val (i, b) = randomByte(e.getX, e.getY)
      randomBytes.update(i, b)
    }
  }

  @FXML
  def mouseReleased(): Unit = {
    val v = fssi.utils.crypto.sha3(randomBytes)
    randomString.value = v.asBytesValue.bcBase58
  }

  @FXML
  def saveAs(): Unit = {
    if (currentAccount.isEmpty) {
      val alert = new Alert(AlertType.Warning) {
        title = "Warning"
        headerText = "No Account Generated"
        contentText = "Please Generate An Account First !"
      }
      alert.showAndWait()
    } else {
      val fc = new FileChooser {}
      Option(fc.showSaveDialog(cCanvasParent.getScene.getWindow)) foreach { file =>
        import better.files._
        file.mkdirs()
        val accountFile      = new java.io.File(file, "account.json").toScala
        val skFile           = new java.io.File(file, "secretKey.json").toScala
        implicit val charset = Charset.forName("utf-8")
        accountFile.overwrite(currentAccount.unsafe().asJson.spaces2)
        skFile.overwrite(currentSec.unsafe().asJson.spaces2)

        val alert = new Alert(AlertType.Information) {
          title = "Account Info"
          headerText = "Account Created"
          contentText = Vector("Account saved to:",
                               accountFile.toString,
                               "\nSecretKey saved to:",
                               skFile.toString).mkString("\r")
        }
        alert.showAndWait()
      }
    }
  }

  @FXML
  def resetAll(): Unit = {
    gc.clearRect(0, 0, gc.getCanvas.getWidth, gc.getCanvas.getHeight)
    currentAccount.clear
    currentSec.clear
    cAccountId.textProperty().value = ""
    cAccountIv.textProperty().value = ""
    cAccountPrv.textProperty().value = ""
    cAccountPub.textProperty().value = ""
    cAccountSec.textProperty().value = ""
    randomString.value = ""
  }

  @FXML
  def generate(): Unit = {
    if (randomString.value.isEmpty) {
      val alert = new Alert(AlertType.Warning) {
        title = "Warning"
        headerText = "No Random Number Created"
        contentText = "Plese drag your mouse on the black area, draw some lines at will."
      }
      alert.showAndWait()
      ()
    } else {
      WorkingThreadPool.delay {
        createAccount() match {
          case Left(t) =>
            Platform.runLater {
              val alert = new Alert(AlertType.Error) {
                title = "error"
                headerText = t.getMessage
                contentText = t.getStackTrace.mkString("\n\t")
              }
              alert.showAndWait()
            }

          case Right((account, sk)) =>
            Platform.runLater {
              currentAccount(account)
              currentSec(sk)
              cAccountId.textProperty().value = account.id.asBytesValue.bcBase58
              cAccountIv.textProperty().value = account.iv.asBytesValue.bcBase58
              cAccountPrv.textProperty().value = account.encPrivKey.asBytesValue.bcBase58
              cAccountPub.textProperty().value = account.pubKey.asBytesValue.bcBase58
              cAccountSec.textProperty().value = sk.asBytesValue.bcBase58
            }
        }
      }
    }
  }

  private def randomByte(x: Double, y: Double): (Int, Byte) = {
    val factor = scala.util.Random.nextDouble() * 1024 + System.currentTimeMillis()
    val i      = scala.util.Random.nextInt(1024)
    (i, BigDecimal(factor + x * y).toByte)
  }

  private def createAccount(): Either[Throwable, (Account, Account.SecretKey)] = {
    val v = fssi.utils.crypto.sha3(randomBytes)
    val p = Program.toolProgram.createAccount(RandomSeed(v))
    runner.runIOAttempt(p, Setting.defaultInstance).unsafeRunSync()
  }

}
