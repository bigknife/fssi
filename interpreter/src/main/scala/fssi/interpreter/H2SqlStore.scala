package fssi
package interpreter

import contract.lib._
import java.sql._

class H2SqlStore(dbUrl: String) extends SqlStore {
  lazy val conn: Connection = DriverManager.getConnection(dbUrl, "", "")

  def commit(): Unit                                                                          = ???
  def rollback(): Unit                                                                        = ???
  def executeCommand(sql: String, args: Object*): Int                                         = ???
  def executeQuery(sql: String, args: Object*): java.util.List[java.util.Map[String, Object]] = ???
  def startTransaction(): Unit                                                                = ???
}
