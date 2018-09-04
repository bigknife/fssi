package fssi.contract.lib;

import java.util.*;

/**
 * a sql-interface contract data store for contract.
 * every contract has their own sql db instance, and can create many tables, excute standard sql sentences (without undetermined parts).
 */
public interface SqlStore {
    /**
     * execute sql command, like update, insert, create table etc.
     * @return the affected line numbers.
     */
    public int executeCommand(String sql, Object... arguments) throws Exception;

    /**
     * execute sql query, like 'select ...'
     * @return a list of column name and value pair.
     */
    public List<Map<String, Object>> executeQuery(String sql, Object... arguments) throws Exception;

    /**
     * start a transaction, if you don't call this, then every sql command will be a transaction.
     */
    public void startTransaction();

    /**
     * commit current transaction
     */
    public void commit();

    /**
     * rollback current transaction
     */
    public void rollback();
}
