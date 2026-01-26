package com.safari.block;

import com.safari.session.SafariSessionManager;
import com.safari.world.SafariDimension;
import com.safari.config.SafariConfig;
import com.safari.economy.SafariEconomy;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SafariPortalBlock extends NetherPortalBlock {
    private static final Map<UUID, Long> lastPortalCheck = new HashMap<>();

    public SafariPortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient) return;
        if (!(entity instanceof ServerPlayerEntity player)) return;

        // Prevent portal usage inside Safari
        if (world.getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) return;

        UUID uuid = player.getUuid();
        long now = System.currentTimeMillis();
        if (now - lastPortalCheck.getOrDefault(uuid, 0L) < 2000) {
            return;
        }

        // Hard check for money before even trying to start session
        int price = SafariConfig.get().entrancePrice;
        if (price > 0 && !SafariEconomy.hasEnough(player, price)) {
            player.sendMessage(Text.translatable("message.safari.need_money_entry", price).formatted(Formatting.RED), true);
            
            // Push player back
            Vec3d velocity = player.getPos().subtract(Vec3d.ofCenter(pos)).normalize().multiply(0.5);
            player.addVelocity(velocity.x, 0.2, velocity.z);
            player.velocityModified = true;
            
            lastPortalCheck.put(uuid, now);
            return;
        }

        if (SafariSessionManager.tryStartSession(player, true)) {
            player.setPortalCooldown(60);
        }
        
        lastPortalCheck.put(uuid, now);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Silence portal ambient sounds/particles.
    }
}
