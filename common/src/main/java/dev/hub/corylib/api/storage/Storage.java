package dev.hub.corylib.api.storage;

public final class Storage {
    public static final StorageType DISK = new StorageType("disk");
    public static final StorageType MEMORY = new StorageType("memory");

    private Storage() {
    }
}
