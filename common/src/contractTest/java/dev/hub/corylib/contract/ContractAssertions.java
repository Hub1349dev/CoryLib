package dev.hub.corylib.contract;

import java.util.Objects;

public final class ContractAssertions {
    private ContractAssertions() {
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void equals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " Expected <" + expected + "> but got <" + actual + ">.");
        }
    }

    public static void notEquals(Object unexpected, Object actual, String message) {
        if (Objects.equals(unexpected, actual)) {
            throw new AssertionError(message + " Both values were <" + actual + ">.");
        }
    }

    public static void throwsException(Class<? extends Throwable> type, ThrowingRunnable action, String message) {
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError(message + " Expected " + type.getName() + " but got " + error.getClass().getName() + ".", error);
        }
        throw new AssertionError(message + " Expected " + type.getName() + " but nothing was thrown.");
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
