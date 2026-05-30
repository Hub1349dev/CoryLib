package dev.hub.corylib.api.builder;

import com.mojang.serialization.Codec;
import dev.hub.corylib.CoryContext;
import dev.hub.corylib.api.entry.DataEntry;
import dev.hub.corylib.api.migration.DataVersion;
import dev.hub.corylib.api.migration.Migration;
import dev.hub.corylib.api.scope.DataScope;
import dev.hub.corylib.api.storage.Storage;
import dev.hub.corylib.api.storage.StorageType;
import dev.hub.corylib.impl.registry.DataRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class DataBuilder<S, T, B extends DataBuilder<S, T, B>> {
    protected final CoryContext context;
    protected final String key;
    protected final Codec<T> codec;
    protected final DataScope<S> scope;
    protected Supplier<T> defaultValue;
    protected StorageType storage = Storage.MEMORY;
    protected Integer version;
    protected final List<Migration> migrations = new ArrayList<>();
    protected Consumer<DataEntry<S, T>> onLoad;
    protected Consumer<DataEntry<S, T>> onSave;

    protected DataBuilder(CoryContext context, String key, Codec<T> codec, DataScope<S> scope) {
        this.context = Objects.requireNonNull(context, "context");
        this.key = validateKey(key);
        this.codec = Objects.requireNonNull(codec, "codec");
        this.scope = Objects.requireNonNull(scope, "scope");
    }

    public B defaultValue(T value) {
        this.defaultValue = () -> value;
        return self();
    }

    public B defaultValue(Supplier<T> supplier) {
        this.defaultValue = Objects.requireNonNull(supplier, "supplier");
        return self();
    }

    public B storage(StorageType storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
        return self();
    }

    public B version(int version) {
        this.version = version;
        return self();
    }

    public B version(int version, Migration migration) {
        this.version = version;
        this.migrations.add(Objects.requireNonNull(migration, "migration"));
        return self();
    }

    public B onLoad(Consumer<DataEntry<S, T>> callback) {
        this.onLoad = Objects.requireNonNull(callback, "callback");
        return self();
    }

    public B onSave(Consumer<DataEntry<S, T>> callback) {
        this.onSave = Objects.requireNonNull(callback, "callback");
        return self();
    }

    public DataEntry<S, T> build() {
        DataRegistry.INSTANCE.assertRegistrationOpen();
        if (defaultValue == null) {
            throw new IllegalStateException("CoryLib entry '" + fullKey() + "' must declare a defaultValue.");
        }
        if (!Storage.DISK.equals(storage) && !Storage.MEMORY.equals(storage)) {
            throw new IllegalStateException("CoryLib entry '" + fullKey() + "' uses unsupported storage type '" + storage.id() + "'.");
        }
        if (Storage.DISK.equals(storage) && version == null) {
            throw new IllegalStateException("CoryLib DISK entry '" + fullKey() + "' must declare .version(int).");
        }
        DataVersion dataVersion = version == null ? null : new DataVersion(version, migrations);
        DataEntry<S, T> entry = createEntry(dataVersion);
        DataRegistry.INSTANCE.register(entry);
        return entry;
    }

    protected DataEntry<S, T> createEntry(DataVersion dataVersion) {
        return new DataEntry<>(context.modId(), key, scope, codec, defaultValue, storage, dataVersion, onLoad, onSave);
    }

    protected String fullKey() {
        return context.modId() + ":" + key;
    }

    protected abstract B self();

    private static String validateKey(String key) {
        Objects.requireNonNull(key, "key");
        if (!key.matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("CoryLib keys must use lowercase path-safe characters: " + key);
        }
        return key;
    }
}
