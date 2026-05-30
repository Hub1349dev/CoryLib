package dev.hub.corylib.api.scope;

import dev.hub.corylib.api.subject.ClientSubject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class Scopes {
    public static final DataScope<MinecraftServer> SERVER = new DataScope<>("server", MinecraftServer.class, true);
    public static final DataScope<ServerLevel> DIMENSION = new DataScope<>("dimension", ServerLevel.class, true);
    public static final DataScope<ServerPlayer> PLAYER = new DataScope<>("player", ServerPlayer.class, true);
    public static final DataScope<ClientSubject> CLIENT = new DataScope<>("client", ClientSubject.class, false);

    private Scopes() {
    }
}
