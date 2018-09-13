package fssi
package interpreter

import org.scalatest._

class H2SqlStoreSpec extends FunSuite {
  test("h2 sql store") {
    val dbUrl = "jdbc:h2:/tmp/h2.test"
    val store = new H2SqlStore(dbUrl)

    store.executeCommand("create table if not exists t_banana (name varchar(100), price double)")
    val c = store.executeCommand("insert into t_banana values (?, ?)", Seq("Fee".asInstanceOf[Object], 100.asInstanceOf[Object]):_ *)
    assert(c == 1)
    val rs = store.executeQuery("select count(1) from t_banana")
    assert(rs.size() == 1)

  }
}
