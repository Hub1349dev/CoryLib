package dev.hub.corylib;

import dev.hub.corylib.api.subject.ClientSubject;
import dev.hub.corylib.impl.registry.DataRegistry;

import java.util.Objects;

public final class CoryData {
    private CoryData() {
    }

    public static CoryContext init(String modId) {
        Objects.requireNonNull(modId, "modId");
        return DataRegistry.INSTANCE.contextFor(modId);
    }

    public static ClientSubject client() {
        return ClientSubject.INSTANCE;
    }
}
