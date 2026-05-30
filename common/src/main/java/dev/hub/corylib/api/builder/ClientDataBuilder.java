package dev.hub.corylib.api.builder;

import com.mojang.serialization.Codec;
import dev.hub.corylib.CoryContext;
import dev.hub.corylib.api.entry.DataEntry;
import dev.hub.corylib.api.migration.DataVersion;
import dev.hub.corylib.api.scope.Scopes;
import dev.hub.corylib.api.subject.ClientSubject;

public final class ClientDataBuilder<T> extends DataBuilder<ClientSubject, T, ClientDataBuilder<T>> {
    private boolean perWorld;

    public ClientDataBuilder(CoryContext context, String key, Codec<T> codec) {
        super(context, key, codec, Scopes.CLIENT);
    }

    public ClientDataBuilder<T> perWorld() {
        this.perWorld = true;
        return this;
    }

    @Override
    protected DataEntry<ClientSubject, T> createEntry(DataVersion dataVersion) {
        return new DataEntry<>(context.modId(), key, scope, codec, defaultValue, storage, dataVersion, onLoad, onSave, null, perWorld);
    }

    @Override
    protected ClientDataBuilder<T> self() {
        return this;
    }
}
