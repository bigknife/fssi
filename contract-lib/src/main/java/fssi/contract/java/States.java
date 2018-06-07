package fssi.contract.java;

import fssi.contract.States$;
import scala.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class States {
    private fssi.contract.States inner;

    private States() {}

    public static  States fromScala(fssi.contract.States scalaStates) {
        States s = new States();
        s.inner = scalaStates;
        return s;
    }

    public fssi.contract.States asScala() {
        return inner;
    }

    public States(AccountState ... accountStates) {
        List<AccountState> states = Arrays.asList(accountStates);
        List<fssi.contract.AccountState> tmp = new ArrayList<>();
        for(AccountState as: states) {
            tmp.add(as.toScala());
        }
        inner = States$.MODULE$.apply(tmp);
    }

    public Optional<AccountState> of(String accountId) {
        if(accountId == null) return Optional.empty();

        Option<fssi.contract.AccountState> as = inner.of(accountId);
        if(as.isDefined()) {
            return Optional.of(AccountState.fromScala(as.get()));
        }
        return Optional.empty();
    }

    public States update(AccountState accountState) {
        if(accountState != null) {
            this.inner = inner.update(accountState.toScala());
        }
        return this;
    }
}
