package dev.hub.corylib.impl.registry;

import dev.hub.corylib.api.scope.DataScope;
import dev.hub.corylib.api.scope.Scopes;

import java.util.*;

public final class ScopeRegistry {
    public static final ScopeRegistry INSTANCE = new ScopeRegistry();

    private final Map<String, DataScope<?>> scopes = new LinkedHashMap<>();

    private ScopeRegistry() {
        register(Scopes.SERVER);
        register(Scopes.DIMENSION);
        register(Scopes.PLAYER);
        register(Scopes.CLIENT);
    }

    public synchronized void register(DataScope<?> scope) {
        Objects.requireNonNull(scope, "scope");
        if (scopes.containsKey(scope.id())) {
            throw new IllegalStateException("CoryLib scope '" + scope.id() + "' is already registered.");
        }
        scopes.put(scope.id(), scope);
    }

    public synchronized Optional<DataScope<?>> byId(String id) {
        return Optional.ofNullable(scopes.get(id));
    }

    public synchronized void assertRegistered(DataScope<?> scope) {
        Objects.requireNonNull(scope, "scope");
        DataScope<?> registered = scopes.get(scope.id());
        if (registered == null) {
            throw new IllegalStateException("CoryLib scope '" + scope.id() + "' is not registered.");
        }
        if (registered != scope) {
            throw new IllegalStateException("CoryLib scope '" + scope.id() + "' must use the registered DataScope instance.");
        }
    }

    public synchronized boolean is(DataScope<?> scope, DataScope<?> expected) {
        assertRegistered(scope);
        return scope.id().equals(expected.id());
    }

    public synchronized Collection<DataScope<?>> all() {
        return List.copyOf(scopes.values());
    }
}
