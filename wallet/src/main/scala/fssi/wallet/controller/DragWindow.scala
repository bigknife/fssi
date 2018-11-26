package fssi.wallet.controller

import scalafx.scene.input.MouseEvent
import scalafx.stage.Stage

class DragWindow {
  private var x: Double = 0
  private var y: Double = 0

  def mousePressed(me: MouseEvent, stage: Stage): Unit = {
    x = me.getScreenX - stage.getX
    y = me.getScreenY - stage.getY
  }

  def mouseDragged(me: MouseEvent, stage: Stage): Unit = {
    stage.x = me.getScreenX - x
    stage.y = me.getScreenY - y
  }
}
