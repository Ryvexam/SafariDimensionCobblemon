package com.safari.block;

import com.safari.session.SafariSessionManager;
import com.safari.world.SafariDimension;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class SafariPortalBlock extends NetherPortalBlock {
    public SafariPortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient) return;
        if (!(entity instanceof ServerPlayerEntity player)) return;

        // Prevent portal usage inside Safari
        if (world.getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) return;

        if (SafariSessionManager.tryStartSession(player, true)) {
            player.setPortalCooldown(60);
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Silence portal ambient sounds/particles.
    }
}
