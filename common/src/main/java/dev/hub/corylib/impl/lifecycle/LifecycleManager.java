package dev.hub.corylib.impl.lifecycle;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.hub.corylib.impl.registry.DataRegistry;
import dev.hub.corylib.impl.sync.CoryNetworking;
import dev.hub.corylib.impl.sync.SyncScheduler;

public final class LifecycleManager {
    private static boolean registered;

    private LifecycleManager() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        CoryNetworking.register();
        LifecycleEvent.SERVER_STARTED.register(server -> {
            DataRegistry.INSTANCE.closeRegistration();
            DataRegistry.INSTANCE.loadServer(server);
        });
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            DataRegistry.INSTANCE.saveServer(server);
            DataRegistry.INSTANCE.flushMemory();
        });
        LifecycleEvent.SERVER_LEVEL_LOAD.register(DataRegistry.INSTANCE::loadLevel);
        LifecycleEvent.SERVER_LEVEL_SAVE.register(DataRegistry.INSTANCE::saveLevel);
        PlayerEvent.PLAYER_JOIN.register(player -> {
            DataRegistry.INSTANCE.closeRegistration();
            DataRegistry.INSTANCE.loadPlayer(player);
        });
        PlayerEvent.PLAYER_QUIT.register(DataRegistry.INSTANCE::savePlayer);
        TickEvent.SERVER_POST.register(SyncScheduler.INSTANCE::flush);
    }
}
