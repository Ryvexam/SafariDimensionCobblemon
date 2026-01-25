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

        ensureSafariNpc(world, new BlockPos(
                SafariWorldState.get().spawnX,
                SafariWorldState.get().spawnY,
                SafariWorldState.get().spawnZ
        ));
    }

    public static void ensureSafariNpcNear(ServerWorld world, BlockPos basePos) {
        ensureSafariNpc(world, basePos);
    }

    private static void ensureSafariNpc(ServerWorld world, BlockPos basePos) {
        if (world.getServer() == null) {
            return;
        }

        if (SafariWorldState.get().guideNpcSpawned) {
            return;
        }

        BlockPos target = new BlockPos(basePos.getX() + 10, basePos.getY(), basePos.getZ());
        world.getServer().execute(() -> {
            world.getChunkManager().getChunk(target.getX() >> 4, target.getZ() >> 4, net.minecraft.world.chunk.ChunkStatus.FULL, true);

            boolean exists = !world.getEntitiesByClass(SafariNpcEntity.class, new Box(target).expand(32.0), entity -> true).isEmpty();
            if (exists) {
                SafariWorldState.get().guideNpcSpawned = true;
                SafariWorldState.get().save();
                return;
            }

            SafariNpcEntity npc = SafariEntities.SAFARI_NPC.create(world);
            if (npc == null) {
                com.safari.SafariMod.LOGGER.warn("Safari NPC spawn failed: entity type not created.");
                return;
            }

            npc.refreshPositionAndAngles(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.0f, 0.0f);
            boolean spawned = world.spawnEntity(npc);
            if (!spawned) {
                com.safari.SafariMod.LOGGER.warn("Safari NPC spawn failed at {},{},{}", target.getX(), target.getY(), target.getZ());
                return;
            }

            SafariWorldState.get().guideNpcSpawned = true;
            SafariWorldState.get().save();
        });
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
