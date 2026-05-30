package dev.hub.corylib;

import dev.hub.corylib.impl.lifecycle.ClientLifecycleManager;
import dev.hub.corylib.impl.lifecycle.LifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Corylib {
    public static final String MOD_ID = "corylib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LifecycleManager.register();
    }

    public static void initClient() {
        ClientLifecycleManager.register();
    }
}
