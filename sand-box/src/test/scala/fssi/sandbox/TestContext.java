package fssi.sandbox;

import fssi.contract.lib.Context;
import fssi.contract.lib.KVStore;
import fssi.contract.lib.SqlStore;
import fssi.contract.lib.TokenQuery;

public class TestContext implements Context {
    @Override
    public KVStore kvStore() {
        System.out.println("Test:Get KVStore");
        return null;
    }

    @Override
    public SqlStore sqlStore() {
        System.out.println("Test:Get SQL Store");
        return null;
    }

    @Override
    public TokenQuery tokenQuery() {
        System.out.println("Test:Get Token Query");
        return null;
    }
}
