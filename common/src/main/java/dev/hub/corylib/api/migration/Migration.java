package dev.hub.corylib.api.migration;

import com.google.gson.JsonElement;

import java.util.Objects;
import java.util.function.Function;

public record Migration(int fromVersion, Function<JsonElement, JsonElement> upgrader) {
    public Migration {
        if (fromVersion < 0) {
            throw new IllegalArgumentException("CoryLib migration version cannot be negative.");
        }
        Objects.requireNonNull(upgrader, "upgrader");
    }

    public static Migration from(int fromVersion, Function<JsonElement, JsonElement> upgrader) {
        return new Migration(fromVersion, upgrader);
    }
}
