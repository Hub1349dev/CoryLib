package dev.hub.corylib.api.scope;

import java.util.Objects;

public final class DataScope<S> {
    private final String id;
    private final Class<S> subjectType;
    private final boolean syncSupported;

    public DataScope(String id, Class<S> subjectType, boolean syncSupported) {
        this.id = Objects.requireNonNull(id, "id");
        this.subjectType = Objects.requireNonNull(subjectType, "subjectType");
        this.syncSupported = syncSupported;
        if (id.isBlank()) {
            throw new IllegalArgumentException("CoryLib scope id cannot be blank.");
        }
    }

    public String id() {
        return id;
    }

    public Class<S> subjectType() {
        return subjectType;
    }

    public boolean syncSupported() {
        return syncSupported;
    }

    @Override
    public String toString() {
        return id;
    }
}
