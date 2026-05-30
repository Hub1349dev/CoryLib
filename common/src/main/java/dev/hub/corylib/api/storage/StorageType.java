package dev.hub.corylib.api.storage;

import java.util.Objects;

public final class StorageType {
    private final String id;

    public StorageType(String id) {
        this.id = Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("CoryLib storage type id cannot be blank.");
        }
    }

    public String id() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof StorageType type && id.equals(type.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
