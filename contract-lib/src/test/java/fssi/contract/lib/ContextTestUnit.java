package fssi.contract.lib;

public class ContextTestUnit {
    public static void main(String[] args) {
        Context context = Context.getInstance();
        context.kvStore();
        context.tokenQuery();
        context.sqlStore();
    }
}
