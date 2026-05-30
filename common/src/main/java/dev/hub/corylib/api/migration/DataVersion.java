package dev.hub.corylib.api.migration;

import com.google.gson.JsonElement;

import java.util.*;

public record DataVersion(int currentVersion, List<Migration> migrations) {
    public DataVersion {
        if (currentVersion < 0) {
            throw new IllegalArgumentException("CoryLib current data version cannot be negative.");
        }
        Objects.requireNonNull(migrations, "migrations");
        Set<Integer> seenVersions = new HashSet<>();
        for (Migration migration : migrations) {
            if (!seenVersions.add(migration.fromVersion())) {
                throw new IllegalArgumentException("Duplicate CoryLib migration from version " + migration.fromVersion());
            }
            if (migration.fromVersion() >= currentVersion) {
                throw new IllegalArgumentException("CoryLib migration from version " + migration.fromVersion() + " cannot target current version " + currentVersion);
            }
        }
        migrations = List.copyOf(migrations);
    }

    public JsonElement migrate(int storedVersion, JsonElement value) {
        if (storedVersion > currentVersion) {
            throw new IllegalStateException("Stored CoryLib data version " + storedVersion + " is newer than supported version " + currentVersion);
        }
        JsonElement current = value;
        int version = storedVersion;
        List<Migration> ordered = migrations.stream()
                .sorted(Comparator.comparingInt(Migration::fromVersion))
                .toList();
        while (version < currentVersion) {
            int target = version;
            Migration migration = ordered.stream()
                    .filter(step -> step.fromVersion() == target)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing migration from version " + target));
            current = migration.upgrader().apply(current);
            version++;
        }
        return current;
    }
}
