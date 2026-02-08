package com.safari.network;

import com.safari.SafariMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.FabricServerConfigurationNetworkHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerConfigurationTask;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class SafariHandshake {
    public static final CustomPayload.Id<VersionRequestPayload> VERSION_REQUEST_ID = new CustomPayload.Id<>(
            Identifier.of(SafariMod.MOD_ID, "version_request")
    );
    public static final CustomPayload.Id<VersionResponsePayload> VERSION_RESPONSE_ID = new CustomPayload.Id<>(
            Identifier.of(SafariMod.MOD_ID, "version_response")
    );
    private static final int TIMEOUT_TICKS = 600;
    private static final int MAX_VERSION_LENGTH = 64;
    private static final ServerPlayerConfigurationTask.Key HANDSHAKE_TASK_KEY =
            new ServerPlayerConfigurationTask.Key(Identifier.of(SafariMod.MOD_ID, "version_handshake").toString());
    private static final Map<ServerConfigurationNetworkHandler, Integer> pending = new ConcurrentHashMap<>();

    private SafariHandshake() {
    }

    public static void initServer() {
        PayloadTypeRegistry.configurationS2C().register(VERSION_REQUEST_ID, VersionRequestPayload.CODEC);
        PayloadTypeRegistry.configurationC2S().register(VERSION_RESPONSE_ID, VersionResponsePayload.CODEC);

        ServerConfigurationNetworking.registerGlobalReceiver(VERSION_RESPONSE_ID, (payload, context) ->
                context.server().execute(() -> handleVersionResponse(context.networkHandler(), payload.version()))
        );

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            String serverVersion = getModVersion();
            if (!ServerConfigurationNetworking.canSend(handler, VERSION_REQUEST_ID)) {
                handler.disconnect(Text.translatable("message.safari.handshake_missing", serverVersion));
                return;
            }

            pending.put(handler, server.getTicks());
            ((FabricServerConfigurationNetworkHandler) handler).addTask(new VersionHandshakeTask(serverVersion));
        });

        ServerConfigurationConnectionEvents.DISCONNECT.register((handler, server) -> pending.remove(handler));

        ServerTickEvents.END_SERVER_TICK.register(SafariHandshake::tickServer);
    }

    private static void handleVersionResponse(ServerConfigurationNetworkHandler handler, String clientVersion) {
        String serverVersion = getModVersion();
        pending.remove(handler);
        if (clientVersion == null || clientVersion.isBlank()) {
            handler.disconnect(Text.translatable("message.safari.handshake_missing", serverVersion));
            return;
        }
        if (!serverVersion.equals(clientVersion)) {
            handler.disconnect(Text.translatable("message.safari.handshake_mismatch", serverVersion, clientVersion));
            return;
        }

        ((FabricServerConfigurationNetworkHandler) handler).completeTask(HANDSHAKE_TASK_KEY);
    }

    private static void tickServer(MinecraftServer server) {
        if (pending.isEmpty()) {
            return;
        }
        int tick = server.getTicks();
        for (Map.Entry<ServerConfigurationNetworkHandler, Integer> entry : pending.entrySet()) {
            if (tick - entry.getValue() < TIMEOUT_TICKS) {
                continue;
            }
            entry.getKey().disconnect(Text.translatable("message.safari.handshake_timeout", getModVersion()));
            pending.remove(entry.getKey());
        }
    }

    private static String getModVersion() {
        return FabricLoader.getInstance()
                .getModContainer(SafariMod.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private record VersionHandshakeTask(String serverVersion) implements ServerPlayerConfigurationTask {
        @Override
        public void sendPacket(Consumer<Packet<?>> sender) {
            sender.accept(ServerConfigurationNetworking.createS2CPacket(new VersionRequestPayload(serverVersion)));
        }

        @Override
        public Key getKey() {
            return HANDSHAKE_TASK_KEY;
        }
    }

    public record VersionRequestPayload(String serverVersion) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, VersionRequestPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.string(MAX_VERSION_LENGTH),
                VersionRequestPayload::serverVersion,
                VersionRequestPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return VERSION_REQUEST_ID;
        }
    }

    public record VersionResponsePayload(String version) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, VersionResponsePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.string(MAX_VERSION_LENGTH),
                VersionResponsePayload::version,
                VersionResponsePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return VERSION_RESPONSE_ID;
        }
    }
}
