package fssi.world.handler

import ch.qos.logback.classic._
import ch.qos.logback.classic.joran.JoranConfigurator
import org.slf4j._
import java.io._

/** logback utils
  *
  */
trait LogbackUtil {
  def setConfig(res: InputStream): Unit = {
    val context: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    val configurator: JoranConfigurator = new JoranConfigurator
    configurator.setContext(context)
    context.reset()
    configurator.doConfigure(res)
  }
}

object LogbackUtil extends LogbackUtil
