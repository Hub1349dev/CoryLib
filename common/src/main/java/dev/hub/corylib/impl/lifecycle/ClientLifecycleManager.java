package dev.hub.corylib.impl.lifecycle;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.hub.corylib.api.subject.ClientSubject;
import dev.hub.corylib.impl.registry.DataRegistry;
import dev.hub.corylib.impl.sync.ClientSyncStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ClientLifecycleManager {
    private static boolean registered;

    private ClientLifecycleManager() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        ClientLifecycleEvent.CLIENT_STARTED.register(client -> {
            ClientSubject.INSTANCE.setGameFolder(client.gameDirectory.toPath());
            DataRegistry.INSTANCE.loadClientGlobal();
        });
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level -> {
            Minecraft minecraft = Minecraft.getInstance();
            ClientSubject.INSTANCE.setGameFolder(minecraft.gameDirectory.toPath());
            ClientSubject.INSTANCE.setWorldFolder(worldFolder(minecraft));
            DataRegistry.INSTANCE.loadClientWorld();
        });
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            DataRegistry.INSTANCE.saveClientWorld();
            DataRegistry.INSTANCE.flushClientWorld();
            ClientSubject.INSTANCE.invalidate();
            ClientSyncStorage.INSTANCE.clear();
        });
        ClientLifecycleEvent.CLIENT_STOPPING.register(client -> {
            DataRegistry.INSTANCE.saveClientWorld();
            DataRegistry.INSTANCE.saveClientGlobal();
            ClientSyncStorage.INSTANCE.clear();
        });
    }

    private static Path worldFolder(Minecraft minecraft) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server != null) {
            return server.getWorldPath(LevelResource.ROOT);
        }

        ServerData serverData = minecraft.getCurrentServer();
        if (serverData != null) {
            return minecraft.gameDirectory.toPath()
                    .resolve("corylib-worlds")
                    .resolve("servers")
                    .resolve(stableServerId(serverData.ip));
        }

        return minecraft.gameDirectory.toPath().resolve("corylib-worlds").resolve("unknown");
    }

    private static String stableServerId(String address) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(address.toLowerCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException error) {
            return Integer.toHexString(address.toLowerCase().hashCode());
        }
    }
}
