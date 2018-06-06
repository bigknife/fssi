package fssi.contract.java;

import fssi.contract.AccountState$;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class AccountState {
    private fssi.contract.AccountState inner;
    private Map<String, AssetCodec<?>> codecs;

    private AccountState() {}

    fssi.contract.AccountState toScala() {
        return inner;
    }
    static AccountState fromScala(fssi.contract.AccountState inner) {
        AccountState inst = new AccountState();
        inst.inner = inner;
        return inst;
    }

    public static AccountState newInstance(String accountId) {
        AccountState inst = new AccountState();
        inst.inner = AccountState$.MODULE$.emptyFor(accountId);
        return inst;
    }

    public <T> void registerAssetCodec(String assetName, AssetCodec<T> codec) {
        if (codecs == null) {
            codecs = new HashMap<>();
        }
        codecs.put(assetName, codec);
    }

    /**
     * add some sort of asset
     * @param assetName name
     * @param asset asset
     * @param <T> asset type
     * @return self
     * @throws NoCodecException if no codec of T was registered, it would throw exceptions
     */
    public <T> AccountState setAsset(String assetName, T asset) throws NoCodecException {
        if (codecs.containsKey(assetName)) {
            AssetCodec<T> codec = (AssetCodec<T>) codecs.get(assetName);
            inner = inner.updateAsset(assetName, codec.encode(asset));
            return this;
        }
        throw new NoCodecException("no codec found for the asset named " + assetName);
    }

    public <T> Optional<T> getAsset(String assetName) throws NoCodecException {
        if (codecs.containsKey(assetName)) {
            scala.Option<byte[]> s = inner.assetOf(assetName);
            AssetCodec<T> codec = (AssetCodec<T>) codecs.get(assetName);
            if (s.isDefined()) {
                return Optional.of(codec.decode(s.get()));
            }
            else return Optional.empty();
        }
        throw new NoCodecException("no codec found for the asset named " + assetName);
    }

}
