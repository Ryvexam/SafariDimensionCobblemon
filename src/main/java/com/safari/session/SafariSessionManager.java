package com.safari.session;

import com.safari.config.SafariConfig;
import com.safari.state.SafariWorldState;
import com.safari.world.SafariDimension;
import com.safari.world.SafariWorldManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SafariSessionManager {
    private static final Map<UUID, SafariSession> activeSessions = new ConcurrentHashMap<>();
    private static final Map<UUID, ResumeSession> pausedSessions = new ConcurrentHashMap<>();
    private static net.minecraft.server.MinecraftServer server;
    private static final Map<UUID, Long> enterDenyCooldown = new ConcurrentHashMap<>();

    public static void setServer(net.minecraft.server.MinecraftServer srv) {
        server = srv;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    public static void startSession(ServerPlayerEntity player) {
        // 1. Validate Dimension
        ServerWorld safariWorld = player.getServer().getWorld(SafariDimension.SAFARI_DIM_KEY);
        if (safariWorld == null) {
            player.sendMessage(Text.of("§cError: Safari Dimension not loaded! Contact Admin."), false);
            return;
        }

        // 2. Ensure Safe Spawn (No Ocean)
        if (!SafariWorldManager.isSafeSpot(safariWorld, new BlockPos(SafariWorldState.get().centerX, 64, SafariWorldState.get().centerZ))) {
             player.sendMessage(Text.of("§eFinding a safe landing spot... please wait."), true);
             SafariWorldManager.findAndSetSafeSpot(safariWorld);
        }

        SafariWorldManager.ensureSpawnPoint(safariWorld);

        // 3. Require at least one empty slot for safari balls
        if (player.getInventory().getEmptySlot() == -1) {
            long now = System.currentTimeMillis();
            long last = enterDenyCooldown.getOrDefault(player.getUuid(), 0L);
            if (now - last > 3000) {
                player.sendMessage(Text.of("§cYou need at least one empty slot for Safari Balls."), false);
                enterDenyCooldown.put(player.getUuid(), now);
            }
            return;
        }

        // 4. Create Session Object with safe return position
        long duration = SafariConfig.get().sessionTimeMinutes * 60L * 20L;
        BlockPos safeReturnPos = findSafeExitPos((ServerWorld) player.getWorld(), getSafeReturnPos(player));
        SafariSession session = new SafariSession(
                player,
                duration,
                player.getWorld().getRegistryKey(),
                safeReturnPos,
                player.getYaw(),
                player.getPitch()
        );
        activeSessions.put(player.getUuid(), session);

        // 5. Give Safari Kit (do not clear inventory)
        SafariInventoryHandler.giveSafariKit(player, SafariConfig.get().initialSafariBalls);

        // 6. Teleport to Safari
        int x = SafariWorldState.get().spawnX;
        int z = SafariWorldState.get().spawnZ;
        int y = SafariWorldState.get().spawnY;
        if (y <= safariWorld.getBottomY()) {
            y = SafariConfig.get().safariSpawnY + SafariConfig.get().safariSpawnOffsetY;
        }
        player.teleport(safariWorld, x + 0.5, y, z + 0.5, 0, 0);

        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 60, 10));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.of("§cSafari Warning")));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.of("§7Leaving ends your session and remaining time")));
        
        // 7. Apply Resistance/Safety for 10 seconds (loading protection)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 255, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 200, 1, false, false));

        player.sendMessage(Text.of("§2Welcome to the Safari Zone! You have " + SafariConfig.get().sessionTimeMinutes + " minutes."), false);
        com.safari.SafariMod.LOGGER.info("Safari enter: {} -> {},{},{}, duration={}m", player.getName().getString(), x, y, z, SafariConfig.get().sessionTimeMinutes);
    }

    public static void endSession(ServerPlayerEntity player) {
        SafariSession session = activeSessions.remove(player.getUuid());
        if (session != null) {
            SafariInventoryHandler.removeSafariBalls(player);
            // 1. Teleport Back (inventory untouched)
            ServerWorld returnWorld = player.getServer().getWorld(session.getReturnDimension());
            if (returnWorld != null) {
                BlockPos safePos = findSafeExitPos(returnWorld, session.getReturnPos());
                player.teleport(returnWorld, 
                    safePos.getX(), 
                    safePos.getY(), 
                    safePos.getZ(), 
                    session.getReturnYaw(), 
                    session.getReturnPitch()
                );
            } else {
                // Fallback to Overworld Spawn if original world is invalid
                ServerWorld overworld = player.getServer().getWorld(World.OVERWORLD);
                player.teleport(overworld, overworld.getSpawnPos().getX(), overworld.getSpawnPos().getY(), overworld.getSpawnPos().getZ(), 0, 0);
            }

            player.sendMessage(Text.of("§cYour Safari session has ended."), false);
            com.safari.SafariMod.LOGGER.info("Safari exit: {} -> {} {} {}", player.getName().getString(), session.getReturnDimension().getValue(), session.getReturnPos().getX(), session.getReturnPos().getY());
        }
    }

    public static void pauseSession(ServerPlayerEntity player) {
        SafariSession session = activeSessions.remove(player.getUuid());
        if (session == null) {
            return;
        }

        if (!player.getWorld().getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) {
            endSession(player);
            return;
        }

        ResumeSession resume = new ResumeSession(
                session.getTicksRemaining(),
                session.getReturnDimension(),
                session.getReturnPos(),
                session.getReturnYaw(),
                session.getReturnPitch(),
                player.getBlockPos(),
                player.getYaw(),
                player.getPitch()
        );
        pausedSessions.put(player.getUuid(), resume);
    }

    public static void resumeSession(ServerPlayerEntity player) {
        ResumeSession resume = pausedSessions.remove(player.getUuid());
        if (resume == null) {
            return;
        }

        ServerWorld safariWorld = player.getServer().getWorld(SafariDimension.SAFARI_DIM_KEY);
        if (safariWorld == null) {
            return;
        }

        SafariSession session = new SafariSession(
                player,
                resume.ticksRemaining(),
                resume.returnDimension(),
                resume.returnPos(),
                resume.returnYaw(),
                resume.returnPitch()
        );
        activeSessions.put(player.getUuid(), session);
        player.teleport(safariWorld,
                resume.safariPos().getX() + 0.5,
                resume.safariPos().getY(),
                resume.safariPos().getZ() + 0.5,
                resume.safariYaw(),
                resume.safariPitch()
        );
    }

    private static void tick() {
        activeSessions.values().forEach(session -> {
            session.tick();
            
            // Action Bar Timer (Every second)
            if (session.getTicksRemaining() % 20 == 0) {
                long seconds = session.getTicksRemaining() / 20;
                long mins = seconds / 60;
                long secs = seconds % 60;
                String color = mins < 5 ? "§c" : "§e";
                session.getPlayer().sendMessage(Text.of(color + "Time Remaining: " + String.format("%02d:%02d", mins, secs)), true);
                if (seconds > 0 && seconds <= 10) {
                    session.getPlayer().playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.8f, 1.4f);
                }
            }

            if (session.getPlayer().isDisconnected()) {
                 // Logic handled in SafariEvents usually, but good to clean up if missed
            }

            if (session.isExpired()) {
                endSession(session.getPlayer());
            }
        });
    }

    public static boolean isInSession(ServerPlayerEntity player) {
        return activeSessions.containsKey(player.getUuid());
    }
    
    public static SafariSession getSession(ServerPlayerEntity player) {
        return activeSessions.get(player.getUuid());
    }

    private record ResumeSession(
            long ticksRemaining,
            RegistryKey<World> returnDimension,
            BlockPos returnPos,
            float returnYaw,
            float returnPitch,
            BlockPos safariPos,
            float safariYaw,
            float safariPitch
    ) {
    }

    private static BlockPos getSafeReturnPos(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        // If standing in portal block, move one block away from portal
        if (isPortalBlock(player.getWorld(), pos)) {
            var dir = player.getHorizontalFacing().getOpposite();
            BlockPos candidate = pos.offset(dir);
            if (player.getWorld().getBlockState(candidate).isAir() && !isPortalBlock(player.getWorld(), candidate)) {
                return candidate;
            }
        }
        return pos;
    }

    private static BlockPos findSafeExitPos(ServerWorld world, BlockPos pos) {
        if (!isPortalBlock(world, pos)) {
            return pos;
        }

        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = pos.add(dx, dy, dz);
                    if (!isPortalBlock(world, candidate) && world.getBlockState(candidate).isAir()) {
                        return candidate;
                    }
                }
            }
        }

        return pos;
    }

    private static boolean isPortalBlock(net.minecraft.world.World world, BlockPos pos) {
        var state = world.getBlockState(pos);
        return state.isOf(com.safari.block.SafariBlocks.SAFARI_PORTAL)
                || state.isOf(com.safari.block.SafariBlocks.SAFARI_PORTAL_FRAME);
    }
}
