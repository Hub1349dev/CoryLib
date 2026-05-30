package dev.hub.corylib.impl.registry;

import dev.hub.corylib.CoryContext;
import dev.hub.corylib.api.entry.DataEntry;
import dev.hub.corylib.api.scope.DataScope;
import dev.hub.corylib.api.scope.Scopes;
import dev.hub.corylib.api.subject.ClientSubject;
import dev.hub.corylib.impl.storage.MemoryStorage;
import dev.hub.corylib.impl.sync.SyncScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class DataRegistry {
    public static final DataRegistry INSTANCE = new DataRegistry();

    private final Map<String, CoryContext> contexts = new LinkedHashMap<>();
    private final List<DataEntry<?, ?>> entries = new ArrayList<>();
    private boolean registrationOpen = true;

    private DataRegistry() {
    }

    public synchronized CoryContext contextFor(String modId) {
        assertRegistrationOpen();
        if (contexts.containsKey(modId)) {
            throw new IllegalStateException("CoryData.init(\"" + modId + "\") was called more than once.");
        }
        CoryContext context = new CoryContext(modId);
        contexts.put(modId, context);
        return context;
    }

    public synchronized void register(DataEntry<?, ?> entry) {
        assertRegistrationOpen();
        Objects.requireNonNull(entry, "entry");
        ScopeRegistry.INSTANCE.assertRegistered(entry.scope());
        if (entries.stream().anyMatch(existing -> existing.id().equals(entry.id()))) {
            throw new IllegalStateException("CoryLib entry '" + entry.id() + "' is already registered.");
        }
        entries.add(entry);
    }

    public synchronized List<DataEntry<?, ?>> entries() {
        return List.copyOf(entries);
    }

    public synchronized DataEntry<?, ?> entryById(String id) {
        return entries.stream()
                .filter(entry -> entry.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    public synchronized void closeRegistration() {
        registrationOpen = false;
    }

    public synchronized void assertRegistrationOpen() {
        if (!registrationOpen) {
            throw new IllegalStateException("CoryLib data registration is closed. Register entries during mod initialization.");
        }
    }

    public synchronized void assertEntryKnown(DataEntry<?, ?> entry) {
        if (!entries.contains(entry)) {
            throw new IllegalStateException("CoryLib entry '" + entry.id() + "' is not registered.");
        }
    }

    public void loadServer(MinecraftServer server) {
        for (DataEntry<?, ?> entry : entries()) {
            if (isScope(entry, Scopes.SERVER)) {
                loadUnchecked(entry, server);
            }
        }
    }

    public void saveServer(MinecraftServer server) {
        for (DataEntry<?, ?> entry : entries()) {
            if (isScope(entry, Scopes.SERVER)) {
                saveUnchecked(entry, server);
            }
        }
    }

    public void loadLevel(ServerLevel level) {
        for (DataEntry<?, ?> entry : entries()) {
            if (isScope(entry, Scopes.DIMENSION)) {
                loadUnchecked(entry, level);
            }
        }
    }

    public void saveLevel(ServerLevel level) {
        for (DataEntry<?, ?> entry : entries()) {
            if (isScope(entry, Scopes.DIMENSION)) {
                saveUnchecked(entry, level);
            }
        }
    }

    public void loadPlayer(ServerPlayer player) {
        for (DataEntry<?, ?> entry : entries()) {
            if (isScope(entry, Scopes.PLAYER)) {
                loadUnchecked(entry, player);
            }
        }
        SyncScheduler.INSTANCE.sendInitial(player);
    }

    public void savePlayer(ServerPlayer player) {
        for (DataEntry<?, ?> entry : entries()) {
            if (isScope(entry, Scopes.PLAYER)) {
                saveUnchecked(entry, player);
                flushUnchecked(entry, player);
            }
        }
    }

    public void loadClientGlobal() {
        loadClient(false);
    }

    public void saveClientGlobal() {
        saveClient(false);
    }

    public void loadClientWorld() {
        loadClient(true);
    }

    public void saveClientWorld() {
        saveClient(true);
    }

    public void flushClientWorld() {
        for (DataEntry<?, ?> entry : entries()) {
            if (isScope(entry, Scopes.CLIENT) && entry.perWorld()) {
                flushUnchecked(entry, ClientSubject.INSTANCE);
            }
        }
    }

    public void flushMemory() {
        MemoryStorage.INSTANCE.clear();
    }

    private void loadClient(boolean perWorld) {
        for (DataEntry<?, ?> entry : entries()) {
            if (isScope(entry, Scopes.CLIENT) && entry.perWorld() == perWorld) {
                loadUnchecked(entry, ClientSubject.INSTANCE);
            }
        }
    }

    private void saveClient(boolean perWorld) {
        for (DataEntry<?, ?> entry : entries()) {
            if (isScope(entry, Scopes.CLIENT) && entry.perWorld() == perWorld) {
                saveUnchecked(entry, ClientSubject.INSTANCE);
            }
        }
    }

    private static <S> void loadUnchecked(DataEntry<?, ?> entry, S subject) {
        ((DataEntry<S, ?>) entry).load(subject);
    }

    private static <S> void saveUnchecked(DataEntry<?, ?> entry, S subject) {
        ((DataEntry<S, ?>) entry).save(subject);
    }

    private static <S> void flushUnchecked(DataEntry<?, ?> entry, S subject) {
        ((DataEntry<S, ?>) entry).flush(subject);
    }

    private static boolean isScope(DataEntry<?, ?> entry, DataScope<?> scope) {
        return ScopeRegistry.INSTANCE.is(entry.scope(), scope);
    }
}
