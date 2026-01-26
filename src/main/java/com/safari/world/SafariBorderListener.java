package com.safari.world;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;

public class SafariBorderListener implements WorldBorderListener {
    private final ServerWorld world;

    public SafariBorderListener(ServerWorld world) {
        this.world = world;
    }

    @Override
    public void onSizeChange(WorldBorder border, double size) {
        broadcast(new WorldBorderSizeChangedS2CPacket(border));
    }

    @Override
    public void onInterpolateSize(WorldBorder border, double fromSize, double toSize, long time) {
        broadcast(new WorldBorderInterpolateSizeS2CPacket(border));
    }

    @Override
    public void onCenterChanged(WorldBorder border, double centerX, double centerZ) {
        broadcast(new WorldBorderCenterChangedS2CPacket(border));
    }

    @Override
    public void onWarningTimeChanged(WorldBorder border, int warningTime) {
        broadcast(new WorldBorderWarningTimeChangedS2CPacket(border));
    }

    @Override
    public void onWarningBlocksChanged(WorldBorder border, int warningBlocks) {
        broadcast(new WorldBorderWarningBlocksChangedS2CPacket(border));
    }

    @Override
    public void onDamagePerBlockChanged(WorldBorder border, double damagePerBlock) {
    }

    @Override
    public void onSafeZoneChanged(WorldBorder border, double safeZone) {
    }

    private void broadcast(Packet<?> packet) {
        if (world == null) return;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.networkHandler != null) {
                player.networkHandler.sendPacket(packet);
            }
        }
    }
}
