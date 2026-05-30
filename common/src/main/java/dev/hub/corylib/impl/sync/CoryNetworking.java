package dev.hub.corylib.impl.sync;

import dev.architectury.networking.simple.MessageType;
import dev.architectury.networking.simple.SimpleNetworkManager;
import dev.hub.corylib.Corylib;

public final class CoryNetworking {
    private static final SimpleNetworkManager NETWORK = SimpleNetworkManager.create(Corylib.MOD_ID);
    public static final MessageType DATA_SYNC = NETWORK.registerS2C("data_sync", DataSyncMessage::new);

    private static boolean registered;

    private CoryNetworking() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        DATA_SYNC.toString();
    }
}
