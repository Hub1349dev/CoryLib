package dev.hub.corylib.impl.storage;

import dev.hub.corylib.api.entry.DataEntry;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class MemoryStorage {
    public static final MemoryStorage INSTANCE = new MemoryStorage();

    private final Map<DataEntry<?, ?>, Map<Object, Object>> values = new IdentityHashMap<>();

    private MemoryStorage() {
    }

    public synchronized <S, T> T getOrDefault(DataEntry<S, T> entry, S subject, Supplier<T> defaultValue) {
        Map<Object, Object> entryValues = values.computeIfAbsent(entry, ignored -> new HashMap<>());
        if (!entryValues.containsKey(subject)) {
            entryValues.put(subject, defaultValue.get());
        }
        return (T) entryValues.get(subject);
    }

    public synchronized <S, T> void set(DataEntry<S, T> entry, S subject, T value) {
        values.computeIfAbsent(entry, ignored -> new HashMap<>()).put(subject, value);
    }

    public synchronized <S, T> boolean has(DataEntry<S, T> entry, S subject) {
        Map<Object, Object> entryValues = values.get(entry);
        return entryValues != null && entryValues.containsKey(subject);
    }

    public synchronized <S, T> void remove(DataEntry<S, T> entry, S subject) {
        Map<Object, Object> entryValues = values.get(entry);
        if (entryValues != null) {
            entryValues.remove(subject);
        }
    }

    public synchronized void clear() {
        values.clear();
    }

    public synchronized <S, T> Map<S, T> getAll(DataEntry<S, T> entry) {
        Map<Object, Object> entryValues = values.getOrDefault(entry, Map.of());
        Map<S, T> copy = new HashMap<>();
        entryValues.forEach((subject, value) -> copy.put((S) subject, (T) value));
        return Map.copyOf(copy);
    }

    public synchronized <S, T> Set<S> subjects(DataEntry<S, T> entry) {
        return (Set<S>) Set.copyOf(values.getOrDefault(entry, Map.of()).keySet());
    }
}
