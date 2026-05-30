package dev.hub.corylib.api.builder;

import com.mojang.serialization.Codec;
import dev.hub.corylib.CoryContext;
import dev.hub.corylib.api.entry.DataEntry;
import dev.hub.corylib.api.migration.DataVersion;
import dev.hub.corylib.api.scope.DataScope;
import dev.hub.corylib.api.sync.SyncTrigger;

import java.util.Objects;

public abstract class SyncedDataBuilder<S, T, B extends SyncedDataBuilder<S, T, B>> extends DataBuilder<S, T, B> {
    protected SyncTrigger syncTrigger;

    protected SyncedDataBuilder(CoryContext context, String key, Codec<T> codec, DataScope<S> scope) {
        super(context, key, codec, scope);
    }

    public B sync(SyncTrigger trigger) {
        this.syncTrigger = Objects.requireNonNull(trigger, "trigger");
        return self();
    }

    @Override
    protected DataEntry<S, T> createEntry(DataVersion dataVersion) {
        return new DataEntry<>(context.modId(), key, scope, codec, defaultValue, storage, dataVersion, onLoad, onSave, syncTrigger, false);
    }
}
