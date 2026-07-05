package dev.hub.corylib.api.entry;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.hub.corylib.api.migration.DataVersion;
import dev.hub.corylib.api.scope.DataScope;
import dev.hub.corylib.api.storage.Storage;
import dev.hub.corylib.api.storage.StorageType;
import dev.hub.corylib.api.sync.SyncTrigger;
import dev.hub.corylib.impl.registry.DataRegistry;
import dev.hub.corylib.impl.storage.JsonFileStorage;
import dev.hub.corylib.impl.storage.MemoryStorage;
import dev.hub.corylib.impl.sync.ClientSyncStorage;
import dev.hub.corylib.impl.sync.SyncScheduler;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DataEntry<S, T> {
    private final String modId;
    private final String key;
    private final DataScope<S> scope;
    private final Codec<T> codec;
    private final Supplier<T> defaultValue;
    private final StorageType storage;
    private final DataVersion version;
    private final Consumer<DataEntry<S, T>> onLoad;
    private final Consumer<DataEntry<S, T>> onSave;
    private final SyncTrigger syncTrigger;
    private final boolean perWorld;
    private final boolean encrypted;

    public DataEntry(
            String modId,
            String key,
            DataScope<S> scope,
            Codec<T> codec,
            Supplier<T> defaultValue,
            StorageType storage,
            DataVersion version,
            Consumer<DataEntry<S, T>> onLoad,
            Consumer<DataEntry<S, T>> onSave
    ) {
        this(modId, key, scope, codec, defaultValue, storage, version, onLoad, onSave, null, false, false);
    }

    public DataEntry(
            String modId,
            String key,
            DataScope<S> scope,
            Codec<T> codec,
            Supplier<T> defaultValue,
            StorageType storage,
            DataVersion version,
            Consumer<DataEntry<S, T>> onLoad,
            Consumer<DataEntry<S, T>> onSave,
            SyncTrigger syncTrigger,
            boolean perWorld
    ) {
        this(modId, key, scope, codec, defaultValue, storage, version, onLoad, onSave, syncTrigger, perWorld, false);
    }

    public DataEntry(
            String modId,
            String key,
            DataScope<S> scope,
            Codec<T> codec,
            Supplier<T> defaultValue,
            StorageType storage,
            DataVersion version,
            Consumer<DataEntry<S, T>> onLoad,
            Consumer<DataEntry<S, T>> onSave,
            SyncTrigger syncTrigger,
            boolean perWorld,
            boolean encrypted
    ) {
        this.modId = Objects.requireNonNull(modId, "modId");
        this.key = Objects.requireNonNull(key, "key");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.version = version;
        this.onLoad = onLoad;
        this.onSave = onSave;
        this.syncTrigger = syncTrigger;
        this.perWorld = perWorld;
        this.encrypted = encrypted;
        if (syncTrigger != null && !scope.syncSupported()) {
            throw new IllegalStateException("CoryLib scope '" + scope.id() + "' does not support server-to-client sync.");
        }
    }

    public T get(S subject) {
        S checked = checkSubject(subject);
        if (!MemoryStorage.INSTANCE.has(this, checked) && Storage.DISK.equals(storage)) {
            load(checked);
        }
        return MemoryStorage.INSTANCE.getOrDefault(this, checked, defaultValue);
    }

    public void set(S subject, T value) {
        S checked = checkSubject(subject);
        MemoryStorage.INSTANCE.set(this, checked, value);
        if (syncTrigger != null) {
            switch (syncTrigger) {
                case IMMEDIATE -> SyncScheduler.INSTANCE.sendNow(checked, this);
                case ON_TICK -> SyncScheduler.INSTANCE.markDirty(checked, this);
                case MANUAL -> {
                }
            }
        }
    }

    public T modify(S subject, Function<T, T> modifier) {
        Objects.requireNonNull(modifier, "modifier");
        T next = modifier.apply(get(subject));
        set(subject, next);
        return next;
    }

    public void clear(S subject) {
        set(subject, defaultValue.get());
    }

    public Map<S, T> getLoaded() {
        return MemoryStorage.INSTANCE.getAll(this);
    }

    public void setLoaded(Function<S, T> valueFactory) {
        Objects.requireNonNull(valueFactory, "valueFactory");
        for (S subject : MemoryStorage.INSTANCE.subjects(this)) {
            set(subject, valueFactory.apply(subject));
        }
    }

    @Deprecated(forRemoval = true)
    public Map<S, T> getAll(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return getLoaded();
    }

    @Deprecated(forRemoval = true)
    public void setAll(MinecraftServer server, Function<S, T> valueFactory) {
        Objects.requireNonNull(server, "server");
        setLoaded(valueFactory);
    }

    public void scheduleSync(S subject) {
        if (syncTrigger != SyncTrigger.MANUAL) {
            throw new IllegalStateException("Entry '" + id() + "' was not built with SyncTrigger.MANUAL.");
        }
        SyncScheduler.INSTANCE.markDirty(checkSubject(subject), this);
    }

    public Optional<T> getSynced() {
        JsonElement value = ClientSyncStorage.INSTANCE.get(id());
        return value == null ? Optional.empty() : Optional.of(decodeValue(value));
    }

    public void load(S subject) {
        S checked = checkSubject(subject);
        if (Storage.DISK.equals(storage)) {
            T value = JsonFileStorage.INSTANCE.load(this, checked).orElseGet(defaultValue);
            MemoryStorage.INSTANCE.set(this, checked, value);
        } else {
            MemoryStorage.INSTANCE.getOrDefault(this, checked, defaultValue);
        }
        if (onLoad != null) {
            onLoad.accept(this);
        }
    }

    public void save(S subject) {
        S checked = checkSubject(subject);
        if (onSave != null) {
            onSave.accept(this);
        }
        if (Storage.DISK.equals(storage) && MemoryStorage.INSTANCE.has(this, checked)) {
            JsonFileStorage.INSTANCE.save(this, checked, get(checked));
        }
    }

    public void flush(S subject) {
        MemoryStorage.INSTANCE.remove(this, checkSubject(subject));
    }

    public String id() {
        return modId + ":" + key;
    }

    public String modId() {
        return modId;
    }

    public String key() {
        return key;
    }

    public DataScope<S> scope() {
        return scope;
    }

    public Codec<T> codec() {
        return codec;
    }

    public StorageType storage() {
        return storage;
    }

    public DataVersion version() {
        return version;
    }

    public SyncTrigger syncTrigger() {
        return syncTrigger;
    }

    public boolean perWorld() {
        return perWorld;
    }

    public boolean encrypted() {
        return encrypted;
    }

    public boolean synced() {
        return syncTrigger != null;
    }

    public JsonElement encodeValue(T value) {
        return codec.encodeStart(JsonOps.INSTANCE, value)
                .getOrThrow(error -> new IllegalStateException("Codec encode failed for '" + id() + "': " + error));
    }

    public T decodeValue(JsonElement value) {
        return codec.parse(JsonOps.INSTANCE, value)
                .getOrThrow(error -> new IllegalStateException("Codec decode failed for '" + id() + "': " + error));
    }

    private S checkSubject(S subject) {
        Objects.requireNonNull(subject, "subject");
        if (!scope.subjectType().isInstance(subject)) {
            throw new IllegalArgumentException("Entry '" + id() + "' expects subject " + scope.subjectType().getName());
        }
        DataRegistry.INSTANCE.assertEntryKnown(this);
        return subject;
    }
}
