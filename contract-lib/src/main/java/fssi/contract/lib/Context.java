package fssi.contract.lib;

/**
 * The Context of runing contract
 */
public interface Context {
    /**
     * return a running-time specified kv store
     */
    KVStore kvStore();

    /**
     * return a running-time specified sql store
     */
    SqlStore sqlStore();

    /**
     * return a running-time specified token querier.
     */
    TokenQuery tokenQuery();

    /**
     * get current invoker's account id
     */
    String currentAccountId();
}
