package fssi
package interpreter

import contract.lib._
import java.sql._

class H2SqlStore(dbUrl: String) extends SqlStore {
  lazy val conn: Connection = DriverManager.getConnection(dbUrl, "", "")

  def close(): Unit = conn.close()

  def commit(): Unit = {
    conn.commit()
    conn.setAutoCommit(true)
  }

  def rollback(): Unit = {
    conn.rollback()
    conn.setAutoCommit(true)
  }

  def executeCommand(sql: String, args: Object*): Int = {
    val statement = conn.prepareStatement(sql)
    setParam(1, args.toSeq, statement)
    statement.executeUpdate()
  }
  def executeQuery(sql: String, args: Object*): java.util.List[java.util.Map[String, Object]] = {
    val statement = conn.prepareStatement(sql)
    setParam(1, args.toSeq, statement)
    val rs = statement.executeQuery()
    rsToList(rs, new java.util.ArrayList[java.util.Map[String, Object]])
  }

  def startTransaction(): Unit = {
    conn.setAutoCommit(false)
  }

  private def setParam(i: Int, args: Seq[Object], statement: PreparedStatement): Unit = {
    if (args.isEmpty) ()
    else {
      statement.setObject(i, args.head)
      setParam(i + 1, args.drop(1), statement)
    }
  }

  private def rsToList(rs: ResultSet, list: java.util.List[java.util.Map[String, Object]])
    : java.util.List[java.util.Map[String, Object]] = {
    if (rs.next) {
      val meta                               = rs.getMetaData
      val columnCount                        = meta.getColumnCount
      val row: java.util.Map[String, Object] = new java.util.HashMap[String, Object]()
      for (i <- 1 to columnCount) {
        val columnName = meta.getColumnName(i)
        val value      = rs.getObject(i)
        row.put(columnName, value)
      }
      list.add(row)
      rsToList(rs, list)
    } else list
  }

}
