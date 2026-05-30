package dev.hub.corylib.api.builder;

import com.mojang.serialization.Codec;
import dev.hub.corylib.CoryContext;
import dev.hub.corylib.api.scope.Scopes;
import net.minecraft.server.MinecraftServer;

public final class ServerDataBuilder<T> extends SyncedDataBuilder<MinecraftServer, T, ServerDataBuilder<T>> {
    public ServerDataBuilder(CoryContext context, String key, Codec<T> codec) {
        super(context, key, codec, Scopes.SERVER);
    }

    @Override
    protected ServerDataBuilder<T> self() {
        return this;
    }
}
