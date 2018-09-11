package fssi.contract.lib;

public class DummyContext implements Context {
    @Override
    public KVStore kvStore() {
        throw new UnsupportedOperationException("No Contract Context Provider Found");
    }

    @Override
    public SqlStore sqlStore() {
        throw new UnsupportedOperationException("No Contract Context Provider Found");
    }

    @Override
    public TokenQuery tokenQuery() {
        throw new UnsupportedOperationException("No Contract Context Provider Found");
    }

    @Override
    public String currentAccountId() {
        throw new UnsupportedOperationException("No Contract Context Provider Found");
    }
}
