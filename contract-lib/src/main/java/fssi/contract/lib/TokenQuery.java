package fssi.contract.lib;

/**
 * query token of an account
 */
public interface TokenQuery {
    /**
     * get amount of an account
     * @return token amount, with the base unit.
     */
    public Long getAmount(String accountId);
}
