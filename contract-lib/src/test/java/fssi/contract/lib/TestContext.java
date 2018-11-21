package fssi.contract.lib;

public class TestContext implements Context {
    @Override
    public KVStore kvStore() {
        System.out.println("Test:Get KVStore");
        return null;
    }

    @Override
    public TokenQuery tokenQuery() {
        System.out.println("Test:Get Token Query");
        return null;
    }

    @Override
    public String currentAccountId() {
        return null;
    }
}
