package dev.hub.corylib.impl.sync;

import dev.hub.corylib.api.entry.DataEntry;
import dev.hub.corylib.api.scope.DataScope;
import dev.hub.corylib.api.scope.Scopes;
import dev.hub.corylib.impl.registry.DataRegistry;
import dev.hub.corylib.impl.registry.ScopeRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class SyncScheduler {
    public static final SyncScheduler INSTANCE = new SyncScheduler();

    private final Map<Object, Set<DataEntry<?, ?>>> dirty = new IdentityHashMap<>();

    private SyncScheduler() {
    }

    public synchronized void markDirty(Object subject, DataEntry<?, ?> entry) {
        dirty.computeIfAbsent(subject, ignored -> new LinkedHashSet<>()).add(entry);
    }

    public void sendNow(Object subject, DataEntry<?, ?> entry) {
        send(subject, List.of(entry));
    }

    public synchronized void flush(MinecraftServer server) {
        Map<Object, Set<DataEntry<?, ?>>> pending = new IdentityHashMap<>(dirty);
        dirty.clear();
        pending.forEach((subject, entries) -> send(subject, List.copyOf(entries)));
    }

    synchronized boolean isDirty(Object subject, DataEntry<?, ?> entry) {
        Set<DataEntry<?, ?>> entries = dirty.get(subject);
        return entries != null && entries.contains(entry);
    }

    synchronized void clearDirty() {
        dirty.clear();
    }

    public void sendInitial(ServerPlayer player) {
        List<DataSyncMessage.Update> updates = new ArrayList<>();
        MinecraftServer server = player.level().getServer();
        for (DataEntry<?, ?> entry : DataRegistry.INSTANCE.entries()) {
            if (!entry.synced()) {
                continue;
            }
            if (isScope(entry, Scopes.PLAYER)) {
                addUpdate(updates, entry, player);
            } else if (isScope(entry, Scopes.SERVER) && server != null) {
                addUpdate(updates, entry, server);
            } else if (isScope(entry, Scopes.DIMENSION)) {
                addUpdate(updates, entry, player.level());
            }
        }
        if (!updates.isEmpty()) {
            new DataSyncMessage(updates).sendTo(player);
        }
    }

    private void send(Object subject, List<DataEntry<?, ?>> entries) {
        if (entries.isEmpty()) {
            return;
        }
        List<DataSyncMessage.Update> updates = new ArrayList<>();
        for (DataEntry<?, ?> entry : entries) {
            if (entry.synced()) {
                addUpdate(updates, entry, subject);
            }
        }
        if (updates.isEmpty()) {
            return;
        }
        DataSyncMessage message = new DataSyncMessage(updates);
        if (subject instanceof ServerPlayer player) {
            message.sendTo(player);
        } else if (subject instanceof ServerLevel level) {
            message.sendToLevel(level);
        } else if (subject instanceof MinecraftServer server) {
            message.sendToAll(server);
        }
    }

    private static <S> void addUpdate(List<DataSyncMessage.Update> updates, DataEntry<?, ?> entry, S subject) {
        DataEntry<S, Object> typed = (DataEntry<S, Object>) entry;
        updates.add(new DataSyncMessage.Update(entry.id(), typed.encodeValue(typed.get(subject))));
    }

    private static boolean isScope(DataEntry<?, ?> entry, DataScope<?> scope) {
        return ScopeRegistry.INSTANCE.is(entry.scope(), scope);
    }
}
