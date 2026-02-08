package com.safari.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class SafariHandshakeClient {
    public static void initClient() {
        ClientConfigurationNetworking.registerGlobalReceiver(SafariHandshake.VERSION_REQUEST_ID, (payload, context) -> {
            String version = FabricLoader.getInstance()
                    .getModContainer("safari")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
            context.responseSender().sendPacket(new SafariHandshake.VersionResponsePayload(version));
        });
    }
}
