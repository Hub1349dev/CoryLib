package dev.hub.corylib.fabric.client;

import dev.hub.corylib.Corylib;
import net.fabricmc.api.ClientModInitializer;

public final class CorylibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Corylib.initClient();
    }
}
