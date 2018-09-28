package com.fssi.sample;

import fssi.contract.lib.Context;
import fssi.contract.lib.KVStore;
import fssi.contract.lib.SqlStore;
import fssi.contract.lib.TokenQuery;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class InterfaceSample {

    public void tokenQuerySample(Context context, String accountId) {
        TokenQuery tokenQuery = context.tokenQuery();
        Long amount = tokenQuery.getAmount(accountId);
    }

    public void kvStoreSample(Context context, String key, String value) {
        KVStore kvStore = context.kvStore();

        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);

        // store to kv store
        kvStore.put(keyBytes, valueBytes);

        // get from kv store
        byte[] vBytes = kvStore.get(keyBytes);
    }

    public void sqlStoreSample(Context context, int arg1, double arg2) {
        try {
            SqlStore sqlStore = context.sqlStore();
            String createTable = "create table if not exists t_arg (arg1 int, arg2 double)";
            // create table
            sqlStore.executeCommand(createTable);

            String queryData = "select arg1,arg2 from t_arg where arg1 = ? and arg2 = ?";
            List<Map<String, Object>> mapList = sqlStore.executeQuery(queryData, arg1, arg2);

            // do something

            // if you want to open transaction try followed method
            sqlStore.startTransaction();
            sqlStore.commit();
            sqlStore.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
