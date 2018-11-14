package fssi.store.bcs

import java.io.{File, FileNotFoundException}

import fssi.store.bcs.types.BCSKey.StateKey
import fssi.store.bcs.types.StateData
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen}

class BCSFeature extends FeatureSpec with BeforeAndAfter with GivenWhenThen {

  var bcs: BCS = _

  before {
    bcs = BCS("/tmp/mybcs")
  }
  after {
    bcs.close()

    def delete(f: File): Unit = {
      if (f.isDirectory) {
        f.listFiles().foreach { f1 =>
          delete(f1)
        }
      }
      if (!f.delete()) throw new FileNotFoundException(s"Failed to delete file: $f")
    }
//    delete(new File("/tmp/mybcs"))
  }

  info("As a BCS(BlockChainStore)")
  info("I want to save data to a separated, snapshot area while validating block")
  info(
    "And, when a block reaches system agreement, I will commit the snapshot data to the persisted area")

  feature("putting data will not affect the persisted area, and only visible in the snapshot area") {
    scenario("put only") {
      Given("a fake account id, whose balance is 0")
      val accountId = "accountId_fake"

      When("put 100 to the account")
      val balance = StateData(BigInt(100).toByteArray)
      bcs.putState(1, StateKey.balance(accountId), balance)

      Then("the account's balance will be 100 in snapshot area")
      val snapBalance = bcs.getSnapshotState(StateKey.balance(accountId))
      assert(snapBalance.isRight)
      assert(snapBalance.right.get.isDefined)
      assert(snapBalance.right.get.get === balance)

      Then("the account's balance is still 0 in persisted area")
      val persistedBalance = bcs.getPersistedState(StateKey.balance(accountId))
      assert(persistedBalance.isRight)
      assert(persistedBalance.right.get.isEmpty)
    }

    scenario("successful transaction in snapshot") {
      Given("two accounts, and give them 100balance every person")
      val accountId1 = "accountId1"
      val accountId2 = "accountId2"
      bcs.putBalance(1, accountId1, 100)
      bcs.putBalance(1, accountId2, 100)
      bcs.commit(1)

      Then("we can retrieve the balances from persisted area")
      val balance1P = bcs.getPersistedBalance(accountId1)
      val balance2P = bcs.getPersistedBalance(accountId2)
      assert(balance1P.right.get == 100)
      assert(balance2P.right.get == 100)

      When("account1 transfer 80 to account2")
      bcs.snapshotTransact { proxy =>
        val b1 = proxy.getBalance(accountId1)
        val b2 = proxy.getBalance(accountId2)
        proxy.putBalance(accountId1, b1 - 80)
        proxy.putBalance(accountId2, b2 + 80)
      }

      Then("in snapshot area, the balance of account1 is 20, and the balance of account2 is 180")
      val b1S = bcs.getSnapshotBalance(accountId1)
      val b2S = bcs.getSnapshotBalance(accountId2)
      assert(b1S.right.get == 20)
      assert(b2S.right.get == 180)

      Then("and, in persisted area, the balance of account1 is still 100, so do account2")
      val b1P = bcs.getPersistedBalance(accountId1)
      val b2P = bcs.getPersistedBalance(accountId2)
      assert(b1P.right.get == 100)
      assert(b2P.right.get == 100)

      When("commit snapshot")
      bcs.commit(1)

      Then(
        "the data in the snapshot area has been moved into the persisted area, " +
          "so balance of the two account in the snapshot area is 0")
      val b1S1 = bcs.getSnapshotBalance(accountId1)
      val b2S1 = bcs.getSnapshotBalance(accountId2)
      assert(b1S1.right.get == 0)
      assert(b2S1.right.get == 0)

      Then("and, persisted balances of the two are: b(accountId1) = 20, b(accountId2) = 180")
      val balance1P1 = bcs.getPersistedBalance(accountId1)
      val balance2P1 = bcs.getPersistedBalance(accountId2)
      assert(balance1P1.right.get == 20)
      assert(balance2P1.right.get == 180)
    }

    scenario("failed transaction in snapshot") {
      Given("two accounts, and give them 100balance every person")
      val accountId1 = "accountId1"
      val accountId2 = "accountId2"
      bcs.putBalance(1, accountId1, 100)
      bcs.putBalance(1, accountId2, 100)
      bcs.commit(1)

      Then("we can retrieve the balances from persisted area")
      val balance1P = bcs.getPersistedBalance(accountId1)
      val balance2P = bcs.getPersistedBalance(accountId2)
      assert(balance1P.right.get == 100)
      assert(balance2P.right.get == 100)

      When("account1 transfer 200 to account2, failed, balance is not enough")
      bcs.snapshotTransact { proxy =>
        val b1 = proxy.getBalance(accountId1)
        val b2 = proxy.getBalance(accountId2)

        val b11 = b1 - 200
        val b21 = b2 + 200

        proxy.putBalance(accountId1, b11)
        proxy.putBalance(accountId2, b21)

        if (b11 < 0 || b21 < 0) throw new RuntimeException("balance of accountId1  is not enough")
        else ()
      }

      Then(
        "transaction failed, no changes. in snapshot area, the balance of account1 is 0, and the balance of account2 is 0")
      val b1S = bcs.getSnapshotBalance(accountId1)
      val b2S = bcs.getSnapshotBalance(accountId2)
      assert(b1S.right.get == 0)
      assert(b2S.right.get == 0)

      Then("and, in persisted area, the balance of account1 is still 100, so do account2")
      val b1P = bcs.getPersistedBalance(accountId1)
      val b2P = bcs.getPersistedBalance(accountId2)
      assert(b1P.right.get == 100)
      assert(b2P.right.get == 100)
    }
  }
}
