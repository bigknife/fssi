package fssi.contract.lib;

public interface KVStore {
    /**
     * put data into the store
     */
    public void put(byte[] key, byte[] value);

    /**
     * get data with some key
     * @return if the key can't be found, return null, or return the data.
     */
    public byte[] get(byte[] key);
}
