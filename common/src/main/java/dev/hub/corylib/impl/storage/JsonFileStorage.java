package dev.hub.corylib.impl.storage;

import com.google.gson.*;
import dev.hub.corylib.Corylib;
import dev.hub.corylib.api.entry.DataEntry;
import dev.hub.corylib.api.migration.DataVersion;
import dev.hub.corylib.impl.subject.SubjectPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class JsonFileStorage {
    public static final JsonFileStorage INSTANCE = new JsonFileStorage();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonFileStorage() {
    }

    public <S, T> Optional<T> load(DataEntry<S, T> entry, S subject) {
        Path path = SubjectPaths.pathFor(entry, subject);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            JsonObject envelope = root.getAsJsonObject();
            JsonElement value = envelope.get("value");
            DataVersion version = entry.version();
            if (version != null && envelope.has("_version")) {
                int storedVersion = envelope.get("_version").getAsInt();
                if (storedVersion < version.currentVersion()) {
                    value = version.migrate(storedVersion, value);
                }
            } else if (version != null && !envelope.has("_version")) {
                throw new IllegalStateException("Stored file for '" + entry.id() + "' has no _version field.");
            }
            return Optional.of(entry.decodeValue(value));
        } catch (Exception error) {
            Corylib.LOGGER.error("Failed to load CoryLib entry {} from {}. Default value will be used.", entry.id(), path, error);
            return Optional.empty();
        }
    }

    public <S, T> void save(DataEntry<S, T> entry, S subject, T value) {
        Path path = SubjectPaths.pathFor(entry, subject);
        try {
            Files.createDirectories(path.getParent());
            JsonObject envelope = new JsonObject();
            if (entry.version() != null) {
                envelope.addProperty("_version", entry.version().currentVersion());
            }
            envelope.add("value", entry.encodeValue(value));
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(envelope, writer);
            }
        } catch (IOException | RuntimeException error) {
            Corylib.LOGGER.error("Failed to save CoryLib entry {} to {}.", entry.id(), path, error);
        }
    }
}
