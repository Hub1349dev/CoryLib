package dev.hub.corylib.impl.subject;

import dev.hub.corylib.api.entry.DataEntry;
import dev.hub.corylib.api.subject.ClientSubject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public final class SubjectPaths {
    private SubjectPaths() {
    }

    public static <S, T> Path pathFor(DataEntry<S, T> entry, S subject) {
        String keyFile = entry.key() + ".json";
        if (subject instanceof MinecraftServer server) {
            return server.getWorldPath(LevelResource.ROOT).resolve("corylib").resolve(entry.modId()).resolve(keyFile);
        }
        if (subject instanceof ServerLevel level) {
            Identifier id = level.dimension().identifier();
            return level.getServer().getWorldPath(LevelResource.ROOT)
                    .resolve("corylib")
                    .resolve(entry.modId())
                    .resolve("dim")
                    .resolve(id.getNamespace())
                    .resolve(id.getPath())
                    .resolve(keyFile);
        }
        if (subject instanceof ServerPlayer player) {
            return player.level().getServer().getWorldPath(LevelResource.ROOT)
                    .resolve("corylib")
                    .resolve(entry.modId())
                    .resolve("player")
                    .resolve(player.getUUID().toString())
                    .resolve(keyFile);
        }
        if (subject instanceof ClientSubject client) {
            Path gameDir = client.gameFolder();
            if (gameDir == null) {
                gameDir = Path.of("corylib-client");
            }
            if (entry.perWorld()) {
                Path world = client.worldFolder();
                if (world == null) {
                    world = gameDir.resolve("corylib-worlds").resolve("unknown");
                }
                return world.resolve("corylib").resolve(entry.modId()).resolve(keyFile);
            }
            return gameDir.resolve("corylib").resolve(entry.modId()).resolve(keyFile);
        }
        throw new IllegalArgumentException("Unsupported CoryLib subject type: " + subject.getClass().getName());
    }
}
