package com.fssi.sample;

import fssi.contract.lib.Context;
import fssi.contract.lib.KVStore;
import fssi.contract.lib.TokenQuery;

import java.nio.charset.StandardCharsets;

public class InterfaceSample {

    public void tokenQuerySample(Context context, String accountId) {
        TokenQuery tokenQuery = context.tokenQuery();
        Long amount = tokenQuery.getAmount(accountId);
        System.out.println("============= SUCCESS: got accountId " + accountId + " token :" + amount + " ==========");

        String currentAccount = context.currentAccountId();
        System.out.println("============= SUCCESS: got current accountId " + currentAccount + " =====");
    }

    public void kvStoreSample(Context context, String key, String value) {
        KVStore kvStore = context.kvStore();

        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);

        // store to kv store
        kvStore.put(keyBytes, valueBytes);
        System.out.println("========= SUCCESS: put key [" + key + "] to store on value [" + value + "] ======");

        // get from kv store
        byte[] vBytes = kvStore.get(keyBytes);
        String storeValue = new String(vBytes, StandardCharsets.UTF_8);
        System.out.println("========= SUCCESS: get key [" + key + "] in store ,found value [" + storeValue + "] =====");
    }

    public void currentAccountIdSample(Context context) {
        String currentAccountId = context.currentAccountId();
        System.out.println("========= SUCCESS: get current account id [" + currentAccountId + "] =====");
    }
}
