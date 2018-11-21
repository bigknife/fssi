package fssi.wallet.controller

import scalafx.beans.property.StringProperty
import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener

private class WorkspaceLoader {
  val workspaceFxmlClasspath: StringProperty = StringProperty("")
}

object WorkspaceLoader {
  private val workspaceLoader = new WorkspaceLoader

  def load(classpath: String): Unit = workspaceLoader.workspaceFxmlClasspath.value = classpath
  def <<(classpath: String): Unit = load(classpath)

  /**
    * add a function to listening the event when fxml classpath changed
    * @param f first parameter is old value, the second one is new value
    */
  def listen(f: (String, String) => Unit): Unit =
    workspaceLoader.workspaceFxmlClasspath.addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String],
                           oldValue: String,
                           newValue: String): Unit = f(oldValue, newValue)
    })
}
