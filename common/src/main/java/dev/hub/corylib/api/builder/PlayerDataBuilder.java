package dev.hub.corylib.api.builder;

import com.mojang.serialization.Codec;
import dev.hub.corylib.CoryContext;
import dev.hub.corylib.api.scope.Scopes;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerDataBuilder<T> extends SyncedDataBuilder<ServerPlayer, T, PlayerDataBuilder<T>> {
    public PlayerDataBuilder(CoryContext context, String key, Codec<T> codec) {
        super(context, key, codec, Scopes.PLAYER);
    }

    @Override
    protected PlayerDataBuilder<T> self() {
        return this;
    }
}
