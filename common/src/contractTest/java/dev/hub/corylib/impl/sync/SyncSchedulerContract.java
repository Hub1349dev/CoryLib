package dev.hub.corylib.impl.sync;

import com.mojang.serialization.Codec;
import dev.hub.corylib.api.entry.DataEntry;
import dev.hub.corylib.api.scope.DataScope;
import dev.hub.corylib.api.storage.Storage;
import dev.hub.corylib.api.sync.SyncTrigger;
import dev.hub.corylib.contract.ContractAssertions;
import dev.hub.corylib.impl.registry.DataRegistry;
import dev.hub.corylib.impl.registry.ScopeRegistry;

import java.util.EnumSet;

public final class SyncSchedulerContract {
    private static final DataScope<String> SUBJECT_SCOPE = new DataScope<>("contract_subject", String.class, true);

    private SyncSchedulerContract() {
    }

    public static void run() {
        ContractAssertions.equals(EnumSet.of(SyncTrigger.IMMEDIATE, SyncTrigger.ON_TICK, SyncTrigger.MANUAL),
                EnumSet.allOf(SyncTrigger.class),
                "Every SyncTrigger value must have an explicit contract test.");

        ScopeRegistry.INSTANCE.register(SUBJECT_SCOPE);
        SyncScheduler.INSTANCE.clearDirty();

        String subject = "subject";
        DataEntry<String, String> immediate = entry("immediate", SyncTrigger.IMMEDIATE);
        DataEntry<String, String> onTick = entry("on_tick", SyncTrigger.ON_TICK);
        DataEntry<String, String> manual = entry("manual", SyncTrigger.MANUAL);

        immediate.set(subject, "value");
        ContractAssertions.isTrue(!SyncScheduler.INSTANCE.isDirty(subject, immediate),
                "IMMEDIATE sync must send now without leaving the entry dirty.");

        onTick.set(subject, "value");
        ContractAssertions.isTrue(SyncScheduler.INSTANCE.isDirty(subject, onTick),
                "ON_TICK sync must mark the entry dirty for the server tick flush.");

        manual.set(subject, "value");
        ContractAssertions.isTrue(!SyncScheduler.INSTANCE.isDirty(subject, manual),
                "MANUAL sync must not mark dirty when the value is set.");

        manual.scheduleSync(subject);
        ContractAssertions.isTrue(SyncScheduler.INSTANCE.isDirty(subject, manual),
                "MANUAL sync must mark dirty only when scheduleSync is called.");
    }

    private static DataEntry<String, String> entry(String key, SyncTrigger trigger) {
        DataEntry<String, String> entry = new DataEntry<>(
                "contract",
                key,
                SUBJECT_SCOPE,
                Codec.STRING,
                () -> "",
                Storage.MEMORY,
                null,
                null,
                null,
                trigger,
                false
        );
        DataRegistry.INSTANCE.register(entry);
        return entry;
    }
}
