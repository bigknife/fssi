package fssi.contract.java;

public interface AssetCodec<T> {
    byte[] encode(T t);
    T decode(byte[] bytes);
}
