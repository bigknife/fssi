package fssi.wallet.controller.workspace
import java.net.URL
import java.util.ResourceBundle

import javafx.fxml._
import javafx.scene.canvas.Canvas
import javafx.scene.input.{MouseEvent => JME}
import javafx.scene.layout.HBox
import scalafx.Includes._
import scalafx.beans.property.StringProperty
import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import fssi.base.BytesValue.implicits._
import fssi.wallet.WorkingThreadPool
import javafx.scene.control.{TextArea, TextField}
import scalafx.application.Platform
import scalafx.stage.FileChooser

class CreateAccountController extends javafx.fxml.Initializable{
  @FXML
  var canvas: Canvas = _
  @FXML
  var canvasParent: HBox = _
  @FXML
  var randomTextArea: TextArea = _
  @FXML
  var savePath: TextField = _

  var gc: GraphicsContext = _


  private val randomBytes = Array.fill(1024)(Byte.MinValue)
  private val randomString = StringProperty("")
  private val savePathProperty = StringProperty("")

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    //val heightProperty = DoubleProperty(0)
    //val widthProperty = DoubleProperty(0)

    canvas.heightProperty() <== canvasParent.heightProperty() - 5
    canvas.widthProperty() <== canvasParent.widthProperty() - 5

    gc = canvas.getGraphicsContext2D

    gc.stroke = Color.web("#F5A623")
    gc.fill = Color.rgb(151,151,151)
    gc.lineWidth = 1

    randomTextArea.textProperty() <== randomString
    savePath.textProperty() <==> savePathProperty
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
    val v = randomBytes.asBytesValue.bcBase58
    Platform.runLater {
      randomString.value = v
    }
  }

  @FXML
  def openFileChooser(): Unit = {
    val fc = new FileChooser
    val file = fc.showSaveDialog(canvasParent.getScene.getWindow)
    savePathProperty.value = file.getAbsolutePath
  }

  private def randomByte(x: Double, y: Double): (Int, Byte) = {
    val factor = scala.util.Random.nextDouble() * 1024 + System.currentTimeMillis()
    val i = scala.util.Random.nextInt(1024)
    (i, BigDecimal(factor + x * y).toByte)
  }

}
