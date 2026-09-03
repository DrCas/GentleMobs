package io.github.drcas.gentlemobs.fabric;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GentleMobsFabric implements ModInitializer {
    public static final String MOD_ID = "gentlemobs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static GentleMode globalMode = GentleMode.PASSIVE;

    @Override
    public void onInitialize() {
        LOGGER.info("GentleMobs Fabric initialized in {} mode.", globalMode);
    }

    public static GentleMode getGlobalMode() {
        return globalMode;
    }

    public static void setGlobalMode(GentleMode mode) {
        globalMode = mode;
    }
}
