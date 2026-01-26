package com.safari;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafariMod implements ModInitializer {
    public static final String MOD_ID = "safari";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Safari Dimension Mod...");
        
        // 1. Register Config Loader (Load on Server Start)
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            com.safari.config.SafariConfig.load(server.getSavePath(WorldSavePath.ROOT).toFile());
            com.safari.world.SafariChunkGenerator.setNoiseParamsLookup(
                    server.getRegistryManager().getOptionalWrapper(RegistryKeys.NOISE_PARAMETERS).orElseThrow()
            );
            com.safari.session.SafariSessionManager.setServer(server);
            com.safari.state.SafariWorldState.load();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                com.safari.session.SafariSessionManager.persistActiveSessionsForShutdown()
        );

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Attach per-dimension border listener only to Safari dimension
            net.minecraft.server.world.ServerWorld safariWorld = server.getWorld(com.safari.world.SafariDimension.SAFARI_DIM_KEY);
            if (safariWorld != null) {
                safariWorld.getWorldBorder().addListener(new com.safari.world.SafariBorderListener(safariWorld));
            }
        });
        
        // 2. Init Session Manager
        com.safari.session.SafariSessionManager.init();
        
        // 3. Register Commands
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(com.safari.command.SafariCommand::register);
        
        // 4. Register Events & Dimension
        com.safari.world.SafariDimension.init();
        com.safari.events.SafariEvents.init();
        
        // 5. Register Blocks & Items
        com.safari.block.SafariBlocks.registerModBlocks();
        com.safari.item.ModItems.registerModItems();

        // 5b. Register Entities
        com.safari.entity.SafariEntities.register();
        com.safari.entity.SafariEntities.registerAttributes();

        // 6. Register Chunk Generator Codec
        Registry.register(Registries.CHUNK_GENERATOR, Identifier.of(MOD_ID, "safari_noise"), com.safari.world.SafariChunkGenerator.CODEC);
    }
}
