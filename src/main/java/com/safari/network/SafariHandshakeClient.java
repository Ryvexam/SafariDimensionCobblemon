package com.safari.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class SafariHandshakeClient {
    public static void initClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String version = FabricLoader.getInstance()
                    .getModContainer("safari")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
            ClientPlayNetworking.send(new SafariHandshake.VersionPayload(version));
        });
    }
}
