package com.safari.world;

import com.safari.config.SafariConfig;
import com.safari.entity.SafariEntities;
import com.safari.entity.SafariNpcEntity;
import com.safari.state.SafariWorldState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.util.math.Box;

public class SafariWorldManager {

    public static void findAndSetSafeSpot(ServerWorld world) {
        SafariWorldState.get().centerX = 0;
        SafariWorldState.get().centerZ = 0;
        SafariWorldState.get().spawnInitialized = false;
        SafariWorldState.get().save();
        updateBorder(world, 0, 0);
        ensureSpawnPoint(world);
    }

    public static void ensureSpawnPoint(ServerWorld world) {
        if (SafariConfig.get().forceCustomSpawn) {
            updateBorder(world, (int) SafariConfig.get().customSpawnX, (int) SafariConfig.get().customSpawnZ);
            return;
        }

        int centerX = SafariWorldState.get().centerX;
        int centerZ = SafariWorldState.get().centerZ;
        world.getChunkManager().getChunk(centerX >> 4, centerZ >> 4, net.minecraft.world.chunk.ChunkStatus.FULL, true);

        if (!SafariWorldState.get().spawnInitialized) {
            int surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ);
            int spawnY = surfaceY + SafariConfig.get().safariSpawnOffsetY;
            if (spawnY <= world.getBottomY() + 1) {
                spawnY = Math.max(SafariConfig.get().safariSpawnY, 64) + SafariConfig.get().safariSpawnOffsetY;
            }

            SafariWorldState.get().spawnX = centerX;
            SafariWorldState.get().spawnY = spawnY;
            SafariWorldState.get().spawnZ = centerZ;
            SafariWorldState.get().spawnInitialized = true;
            SafariWorldState.get().save();
        }

        updateBorder(world, SafariWorldState.get().spawnX, SafariWorldState.get().spawnZ);
    }

    public static void ensureSafariNpcNear(ServerWorld world, BlockPos basePos) {
        // No longer auto-spawning
    }

    private static void ensureSafariNpc(ServerWorld world, BlockPos basePos) {
        // No longer auto-spawning
    }

    private static void updateBorder(ServerWorld world, int x, int z) {
        world.getWorldBorder().setCenter(x, z);
        world.getWorldBorder().setSize(SafariConfig.get().dimensionSize);
    }

    public static boolean isSafeSpot(ServerWorld world, BlockPos pos) {
        return isValidBiome(world, pos);
    }

    private static boolean isValidBiome(ServerWorld world, BlockPos pos) {
        var biomeEntry = world.getBiome(pos);
        Identifier biomeId = world.getRegistryManager().get(RegistryKeys.BIOME).getId(biomeEntry.value());
        if (biomeId == null) return false;

        var allowed = SafariConfig.get().allowedBiomes;
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }

        String idStr = biomeId.toString();
        for (String entry : allowed) {
            if (idStr.equals(entry)) return true;
        }

        return false;
    }
}
