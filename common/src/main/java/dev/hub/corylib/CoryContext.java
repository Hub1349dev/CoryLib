package dev.hub.corylib;

import com.mojang.serialization.Codec;
import dev.hub.corylib.api.builder.ClientDataBuilder;
import dev.hub.corylib.api.builder.DimensionDataBuilder;
import dev.hub.corylib.api.builder.PlayerDataBuilder;
import dev.hub.corylib.api.builder.ServerDataBuilder;
import dev.hub.corylib.impl.registry.DataRegistry;

import java.util.Objects;

public final class CoryContext {
    private final String modId;

    public CoryContext(String modId) {
        this.modId = Objects.requireNonNull(modId, "modId");
    }

    public String modId() {
        return modId;
    }

    public <T> ServerDataBuilder<T> server(String key, Codec<T> codec) {
        DataRegistry.INSTANCE.assertRegistrationOpen();
        return new ServerDataBuilder<>(this, key, codec);
    }

    public <T> DimensionDataBuilder<T> dimension(String key, Codec<T> codec) {
        DataRegistry.INSTANCE.assertRegistrationOpen();
        return new DimensionDataBuilder<>(this, key, codec);
    }

    public <T> PlayerDataBuilder<T> player(String key, Codec<T> codec) {
        DataRegistry.INSTANCE.assertRegistrationOpen();
        return new PlayerDataBuilder<>(this, key, codec);
    }

    public <T> ClientDataBuilder<T> client(String key, Codec<T> codec) {
        DataRegistry.INSTANCE.assertRegistrationOpen();
        return new ClientDataBuilder<>(this, key, codec);
    }

    public <T> ServerDataBuilder<T> scopeServer(String key, Codec<T> codec) {
        return server(key, codec);
    }

    public <T> DimensionDataBuilder<T> scopeDimension(String key, Codec<T> codec) {
        return dimension(key, codec);
    }

    public <T> PlayerDataBuilder<T> scopePlayer(String key, Codec<T> codec) {
        return player(key, codec);
    }
}
