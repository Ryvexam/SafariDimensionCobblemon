package com.safari.session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.safari.config.SafariConfig;
import com.safari.state.SafariWorldState;
import com.safari.world.SafariDimension;
import com.safari.world.SafariWorldManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.sound.SoundEvents;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SafariSessionManager {
    private static final Map<UUID, SafariSession> activeSessions = new ConcurrentHashMap<>();
    private static final Map<UUID, ResumeSession> pausedSessions = new ConcurrentHashMap<>();
    private static net.minecraft.server.MinecraftServer server;
    private static final Map<UUID, Long> enterDenyCooldown = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void setServer(net.minecraft.server.MinecraftServer srv) {
        server = srv;
        loadPausedSessions();
    }

    public static void persistActiveSessionsForShutdown() {
        activeSessions.forEach((uuid, session) -> {
            if (pausedSessions.containsKey(uuid)) {
                return;
            }
            ServerPlayerEntity player = session.getPlayer();
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
            pausedSessions.put(uuid, resume);
        });
        savePausedSessions();
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    public static void startSession(ServerPlayerEntity player) {
        // 1. Validate Dimension
        ServerWorld safariWorld = player.getServer().getWorld(SafariDimension.SAFARI_DIM_KEY);
        if (safariWorld == null) {
            player.sendMessage(Text.translatable("message.safari.dimension_not_loaded").formatted(Formatting.RED), false);
            return;
        }

        // 2. Ensure Safe Spawn (No Ocean)
        if (!SafariWorldManager.isSafeSpot(safariWorld, new BlockPos(SafariWorldState.get().centerX, 64, SafariWorldState.get().centerZ))) {
             player.sendMessage(Text.translatable("message.safari.finding_safe_spot").formatted(Formatting.YELLOW), true);
             SafariWorldManager.findAndSetSafeSpot(safariWorld);
        }

        SafariWorldManager.ensureSpawnPoint(safariWorld);

        // 3. Require at least one empty slot for safari balls
        if (player.getInventory().getEmptySlot() == -1) {
            long now = System.currentTimeMillis();
            long last = enterDenyCooldown.getOrDefault(player.getUuid(), 0L);
            if (now - last > 3000) {
                player.sendMessage(Text.translatable("message.safari.need_empty_slot").formatted(Formatting.RED), false);
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

        int fadeInTicks = Math.max(0, SafariConfig.get().entryTitleFadeInTicks);
        int stayTicks = Math.max(0, SafariConfig.get().entryTitleStayTicks);
        int fadeOutTicks = Math.max(0, SafariConfig.get().entryTitleFadeOutTicks);
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeInTicks, stayTicks, fadeOutTicks));
        String entryTitle = SafariConfig.get().entryTitle;
        String entrySubtitle = SafariConfig.get().entrySubtitle;
        if (entryTitle != null && !entryTitle.isBlank()) {
            player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(entryTitle)));
        }
        if (entrySubtitle != null && !entrySubtitle.isBlank()) {
            player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(entrySubtitle)));
        }
        
        // 7. Apply Resistance/Safety for 10 seconds (loading protection)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 255, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 200, 1, false, false));

        player.sendMessage(
                Text.translatable("message.safari.welcome", SafariConfig.get().sessionTimeMinutes)
                        .formatted(Formatting.DARK_GREEN),
                false
        );
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
                player.setPortalCooldown(80);
            } else {
                // Fallback to Overworld Spawn if original world is invalid
                ServerWorld overworld = player.getServer().getWorld(World.OVERWORLD);
                player.teleport(overworld, overworld.getSpawnPos().getX(), overworld.getSpawnPos().getY(), overworld.getSpawnPos().getZ(), 0, 0);
            }

            player.sendMessage(Text.translatable("message.safari.session_ended").formatted(Formatting.RED), false);
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
        savePausedSessions();
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
        savePausedSessions();
    }

    private static void tick() {
        activeSessions.values().forEach(session -> {
            session.tick();

            if (!session.getPlayer().getWorld().getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) {
                endSession(session.getPlayer());
                return;
            }
            
            // Action Bar Timer (Every second)
            if (session.getTicksRemaining() % 20 == 0) {
                long seconds = session.getTicksRemaining() / 20;
                long mins = seconds / 60;
                long secs = seconds % 60;
                Text timeText = Text.translatable("message.safari.time_remaining", mins, secs);
                timeText = timeText.formatted(mins < 5 ? Formatting.RED : Formatting.YELLOW);
                session.getPlayer().sendMessage(timeText, true);
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

        if (server != null) {
            ServerWorld safariWorld = server.getWorld(SafariDimension.SAFARI_DIM_KEY);
            if (safariWorld != null) {
                for (ServerPlayerEntity player : safariWorld.getPlayers()) {
                    UUID uuid = player.getUuid();
                    if (!activeSessions.containsKey(uuid) && !pausedSessions.containsKey(uuid)) {
                        ServerWorld overworld = server.getWorld(World.OVERWORLD);
                        if (overworld != null) {
                            player.teleport(overworld,
                                    overworld.getSpawnPos().getX(),
                                    overworld.getSpawnPos().getY(),
                                    overworld.getSpawnPos().getZ(),
                                    player.getYaw(),
                                    player.getPitch()
                            );
                            player.sendMessage(Text.translatable("message.safari.not_in_session").formatted(Formatting.RED), false);
                        }
                    }
                }
            }
        }
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

    private record PersistedSession(
            long ticksRemaining,
            String returnDimension,
            int returnX,
            int returnY,
            int returnZ,
            float returnYaw,
            float returnPitch,
            int safariX,
            int safariY,
            int safariZ,
            float safariYaw,
            float safariPitch
    ) {
    }

    private static File getSessionsFile() {
        if (server == null) {
            return null;
        }
        return server.getSavePath(WorldSavePath.ROOT).resolve("safari-sessions.json").toFile();
    }

    private static void loadPausedSessions() {
        File file = getSessionsFile();
        if (file == null || !file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            PersistedSessions persisted = GSON.fromJson(reader, PersistedSessions.class);
            if (persisted == null || persisted.sessions == null) {
                return;
            }
            pausedSessions.clear();
            for (var entry : persisted.sessions.entrySet()) {
                UUID uuid = UUID.fromString(entry.getKey());
                PersistedSession data = entry.getValue();
                RegistryKey<World> returnDimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(data.returnDimension()));
                BlockPos returnPos = new BlockPos(data.returnX(), data.returnY(), data.returnZ());
                BlockPos safariPos = new BlockPos(data.safariX(), data.safariY(), data.safariZ());
                pausedSessions.put(uuid, new ResumeSession(
                        data.ticksRemaining(),
                        returnDimension,
                        returnPos,
                        data.returnYaw(),
                        data.returnPitch(),
                        safariPos,
                        data.safariYaw(),
                        data.safariPitch()
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void savePausedSessions() {
        File file = getSessionsFile();
        if (file == null) {
            return;
        }

        PersistedSessions persisted = new PersistedSessions();
        persisted.sessions = new HashMap<>();
        for (var entry : pausedSessions.entrySet()) {
            UUID uuid = entry.getKey();
            ResumeSession data = entry.getValue();
            PersistedSession session = new PersistedSession(
                    data.ticksRemaining(),
                    data.returnDimension().getValue().toString(),
                    data.returnPos().getX(),
                    data.returnPos().getY(),
                    data.returnPos().getZ(),
                    data.returnYaw(),
                    data.returnPitch(),
                    data.safariPos().getX(),
                    data.safariPos().getY(),
                    data.safariPos().getZ(),
                    data.safariYaw(),
                    data.safariPitch()
            );
            persisted.sessions.put(uuid.toString(), session);
        }

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(persisted, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final class PersistedSessions {
        private Map<String, PersistedSession> sessions = new HashMap<>();
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
        if (!isPortalBlock(world, pos) && !isPortalNearby(world, pos)) {
            return pos;
        }

        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos candidate = pos.add(dx, dy, dz);
                    if (!isPortalBlock(world, candidate)
                            && !isPortalNearby(world, candidate)
                            && world.getBlockState(candidate).isAir()
                            && world.getBlockState(candidate.up()).isAir()) {
                        return candidate;
                    }
                }
            }
        }

        return pos;
    }

    private static boolean isPortalNearby(ServerWorld world, BlockPos pos) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (isPortalBlock(world, pos.add(dx, dy, dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isPortalBlock(net.minecraft.world.World world, BlockPos pos) {
        var state = world.getBlockState(pos);
        return state.isOf(com.safari.block.SafariBlocks.SAFARI_PORTAL)
                || state.isOf(com.safari.block.SafariBlocks.SAFARI_PORTAL_FRAME);
    }
}
