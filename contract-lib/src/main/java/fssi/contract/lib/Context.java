package fssi.contract.lib;

/**
 * The Context of runing contract
 */
public interface Context {
    /**
     * return a running-time specified kv store
     */
    public KVStore getKVStore();

    /**
     * return a running-time specified sql store
     */
    public SqlStore getSqlStore();

    /**
     * return a running-time specified token querier.
     */
    public TokenQuery tokenQuery();
}
