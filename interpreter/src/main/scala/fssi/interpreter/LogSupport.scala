package fssi
package interpreter

import org.slf4j._

/** with LogSupport, we can use `log` to output some logs
  */
trait LogSupport {
  lazy val log = LoggerFactory.getLogger(getClass)
}
