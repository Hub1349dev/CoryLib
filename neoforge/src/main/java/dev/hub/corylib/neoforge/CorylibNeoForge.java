package dev.hub.corylib.neoforge;

import dev.hub.corylib.Corylib;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.common.Mod;

@Mod(Corylib.MOD_ID)
public final class CorylibNeoForge {
    public CorylibNeoForge() {
        // Run our common setup.
        Corylib.init();
        if (FMLEnvironment.dist.isClient()) {
            Corylib.initClient();
        }
    }
}
