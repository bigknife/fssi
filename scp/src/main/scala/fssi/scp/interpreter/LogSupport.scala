package fssi.scp
package interpreter

import org.slf4j._

trait LogSupport {
  lazy val log = LoggerFactory.getLogger(getClass)
}
