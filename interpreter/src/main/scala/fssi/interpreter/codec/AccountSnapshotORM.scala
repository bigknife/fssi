package fssi.interpreter.codec

import java.sql.ResultSet

import fssi.ast.domain.types.{Account, BytesValue, Token}
import fssi.interpreter.util.SnapshotDB

trait AccountSnapshotORM {
  implicit val accountSnapshotORM: SnapshotDB.ORM[Account.Snapshot] = new SnapshotDB.ORM[Account.Snapshot] {
    override def to(result: ResultSet): Account.Snapshot = {
      Account.Snapshot(
        account = Account(
          id = Account.ID(result.getObject[String](1, classOf[String])),
          privateKeyData = BytesValue.Empty,
          publicKeyData = BytesValue.decodeHex(result.getObject[String](2, classOf[String])),
          iv = BytesValue.decodeHex(result.getObject[String](3, classOf[String])),
          balance = Token.tokenWithBaseUnit(result.getLong(4))
        ),
        timestamp = result.getLong(5),
        status = Account.Snapshot(result.getObject[String](6, classOf[String]))
      )
    }
  }
}
