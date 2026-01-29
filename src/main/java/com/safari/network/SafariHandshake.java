package com.safari.network;

import com.safari.SafariMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SafariHandshake {
    public static final CustomPayload.Id<VersionPayload> VERSION_ID = new CustomPayload.Id<>(
            Identifier.of(SafariMod.MOD_ID, "version")
    );
    private static final int TIMEOUT_TICKS = 100;
    private static final int MAX_VERSION_LENGTH = 64;
    private static final Map<UUID, Integer> pending = new ConcurrentHashMap<>();

    private SafariHandshake() {
    }

    public static void initServer() {
        PayloadTypeRegistry.playC2S().register(VERSION_ID, VersionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(VERSION_ID, (payload, context) ->
                context.server().execute(() -> handleVersion(context.player(), payload.version()))
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                pending.put(handler.getPlayer().getUuid(), server.getTicks())
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                pending.remove(handler.getPlayer().getUuid())
        );

        ServerTickEvents.END_SERVER_TICK.register(SafariHandshake::tickServer);
    }

    public static void initClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientPlayNetworking.send(new VersionPayload(getModVersion()));
        });
    }

    private static void handleVersion(ServerPlayerEntity player, String clientVersion) {
        String serverVersion = getModVersion();
        pending.remove(player.getUuid());
        if (clientVersion == null || clientVersion.isBlank()) {
            player.networkHandler.disconnect(Text.translatable("message.safari.handshake_missing", serverVersion));
            return;
        }
        if (!serverVersion.equals(clientVersion)) {
            player.networkHandler.disconnect(Text.translatable("message.safari.handshake_mismatch", serverVersion, clientVersion));
        }
    }

    private static void tickServer(MinecraftServer server) {
        if (pending.isEmpty()) {
            return;
        }
        int tick = server.getTicks();
        for (Map.Entry<UUID, Integer> entry : pending.entrySet()) {
            if (tick - entry.getValue() < TIMEOUT_TICKS) {
                continue;
            }
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                player.networkHandler.disconnect(Text.translatable("message.safari.handshake_missing", getModVersion()));
            }
            pending.remove(entry.getKey());
        }
    }

    private static String getModVersion() {
        return FabricLoader.getInstance()
                .getModContainer(SafariMod.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    public record VersionPayload(String version) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, VersionPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.string(MAX_VERSION_LENGTH),
                VersionPayload::version,
                VersionPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return VERSION_ID;
        }
    }
}
