package dev.hub.corylib.impl.sync;

import com.google.gson.JsonElement;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientSyncStorage {
    public static final ClientSyncStorage INSTANCE = new ClientSyncStorage();

    private final Map<String, JsonElement> values = new LinkedHashMap<>();

    private ClientSyncStorage() {
    }

    public synchronized void put(String entryId, JsonElement value) {
        values.put(entryId, value.deepCopy());
    }

    public synchronized JsonElement get(String entryId) {
        JsonElement value = values.get(entryId);
        return value == null ? null : value.deepCopy();
    }

    public synchronized Map<String, JsonElement> snapshot() {
        Map<String, JsonElement> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(key, value.deepCopy()));
        return Map.copyOf(copy);
    }

    public synchronized void clear() {
        values.clear();
    }
}
