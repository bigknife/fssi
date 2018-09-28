package fssi
package contract
package scaffold
package inf

import org.slf4j.{Logger, LoggerFactory}

trait BaseLogger {

  protected lazy val logger: Logger = LoggerFactory.getLogger(getClass)
}
