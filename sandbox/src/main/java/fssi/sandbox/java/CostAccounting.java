package fssi.sandbox.java;

import fssi.sandbox.CostAccounting$;

public class CostAccounting {
    public static void recordThrow() {
        CostAccounting$.MODULE$.recordThrow();
    }

    public static void recordArrayAllocation(final int length, final int multiplier) {
        CostAccounting$.MODULE$.recordArrayAllocation(length, multiplier);
    }

    public static void recordAllocation(String className) {
        CostAccounting$.MODULE$.recordAllocation(className);
    }

    public static void recordJump() {
        CostAccounting$.MODULE$.recordJump();
    }

    public static void recordMethodCall() {
        CostAccounting$.MODULE$.recordMethodCall();
    }

    public static long throwCost() {
        return CostAccounting$.MODULE$.throwCost();
    }

    public static long allocationCost() {
        return CostAccounting$.MODULE$.allocationCost();
    }

    public static long jumpCost() {
        return CostAccounting$.MODULE$.jumpCost();
    }

    public static long methodCallCost() {
        return CostAccounting$.MODULE$.methodCallCost();
    }
}
