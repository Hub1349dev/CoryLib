package dev.hub.corylib.api.builder;

import com.mojang.serialization.Codec;
import dev.hub.corylib.CoryContext;
import dev.hub.corylib.api.scope.Scopes;
import net.minecraft.server.level.ServerLevel;

public final class DimensionDataBuilder<T> extends SyncedDataBuilder<ServerLevel, T, DimensionDataBuilder<T>> {
    public DimensionDataBuilder(CoryContext context, String key, Codec<T> codec) {
        super(context, key, codec, Scopes.DIMENSION);
    }

    @Override
    protected DimensionDataBuilder<T> self() {
        return this;
    }
}
