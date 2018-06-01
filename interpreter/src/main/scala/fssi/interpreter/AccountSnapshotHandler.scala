package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._
import fssi.interpreter.util.SnapshotDB
import fssi.interpreter.orm._
/**
  * Account Snapshot is a store, which save some snapshot info of account.
  * of course, no sensitive info should be stored. so check out that it was desensitized.
  */
class AccountSnapshotHandler extends AccountSnapshot.Handler[Stack] {
  private val ACCOUNT_TABLE_NAME: String = "ACCOUNT"
  private val SQL_CreateTable: String = s"CREATE TABLE IF NOT EXISTS $ACCOUNT_TABLE_NAME (" +
    s"id varchar(255) not null primary key," +
    s"publ varchar(512) not null," +
    s"iv varchar(255) not null," +
    s"balance bigint not null," +
    s"timestamp bigint not null" +
    s")"
  private val SQL_UpsertAccountSnapshot: String = s"MERGE INTO $ACCOUNT_TABLE_NAME " +
    s"KEY(id) VALUES (?, ?, ?, ?, ?)"
  private val Sql_SelectAccountSnapshot
    : String = s"SELECT id, publ, iv, balance, timestamp FROM $ACCOUNT_TABLE_NAME " +
    s"WHERE id=?"

  override def saveSnapshot(snapshot: Account.Snapshot): Stack[Account.Snapshot] = Stack {
    setting =>
      SnapshotDB.initOnDemand(setting.snapshotDbBaseDir,
                              startWebConsole = setting.startSnapshotDbConsole)

      // create table
      SnapshotDB.executeCommand(SQL_CreateTable)

      // insert or update
      val acc = snapshot.account
      SnapshotDB.executeCommand(SQL_UpsertAccountSnapshot,
                                acc.id.value,
                                acc.publicKeyData.hex,
                                acc.iv.hex,
                                acc.balance.amount,
                                snapshot.timestamp)

      snapshot
  }

  override def findAccountSnapshot(id: Account.ID): Stack[Option[Account.Snapshot]] = Stack {
    setting =>
      SnapshotDB.initOnDemand(setting.snapshotDbBaseDir,
                              startWebConsole = setting.startSnapshotDbConsole)

      //query
      SnapshotDB.executeQuery[Account.Snapshot](Sql_SelectAccountSnapshot, id.value).headOption
  }
}

object AccountSnapshotHandler {

  trait Implicits {
    implicit val accountSnapshot: AccountSnapshotHandler = new AccountSnapshotHandler {}
  }
  object implicits extends Implicits
}
