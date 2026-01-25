package com.safari.logic;

import com.safari.config.SafariConfig;
import com.safari.world.SafariDimension;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Random;

public class SafariCaptureLogic {
    private static final Random RANDOM = new Random();

    // Returns TRUE if we handled it (and cancelled original logic), FALSE if we should let Cobblemon proceed.
    public static boolean onBallHit(ProjectileEntity ball, EntityHitResult result) {
        Entity owner = ball.getOwner();
        if (!(owner instanceof ServerPlayerEntity player)) return false;

        // 1. Check Dimension
        if (!player.getWorld().getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) {
            return false; // Not in Safari, normal behavior
        }

        // 2. Identify Target (Is it a Pokemon?)
        Entity target = result.getEntity();
        String targetClass = target.getClass().getName();
        if (!targetClass.contains("PokemonEntity")) {
            return false; // Not a pokemon
        }

        // 3. Identify Ball Type
        // We assume the ball entity has a way to identify its item.
        // For now, we assume ALL balls thrown in Safari are valid because we restricted the inventory.
        // But if they smuggled a Master Ball, we might want to block it.
        // Since we can't easily check the NBT of the flying entity without casting to Cobblemon classes,
        // we will rely on the Inventory Restriction (Phase 2/3) which already clears inventory.
        // So any ball thrown IS a Safari Ball.

        // 4. Calculate Catch Rate
        // We need to know the species catch rate.
        // Since we can't access "pokemon.getForm().getCatchRate()" easily via reflection without being messy,
        // We will use a simplified "Rarity" based on our config or default to "Common".
        
        // TODO: Try to reflectively get catch rate. For now, flat rate or random.
        double rate = getCatchRate(target);
        
        boolean caught = RANDOM.nextDouble() < rate;
        
        if (caught) {
            player.sendMessage(Text.translatable("message.safari.capture_success").formatted(Formatting.GREEN), true);
            // Execute Capture
            // target.giveToPlayer(player); // Pseudo-code
            executeCapture(target, player);
            target.discard(); // Remove from world
        } else {
            player.sendMessage(Text.translatable("message.safari.capture_fail").formatted(Formatting.RED), true);
            // Ball is consumed (we just don't refund it).
        }

        ball.discard(); // Destroy the ball entity so it doesn't trigger Cobblemon logic
        return true; // We handled it
    }

    private static double getCatchRate(Entity pokemon) {
        // Attempt to read catch rate via Reflection if possible, else use Common config
        // Defaulting to Common for prototype safety
        return SafariConfig.get().commonCatchRate;
    }
    
    private static void executeCapture(Entity pokemon, ServerPlayerEntity player) {
        // Reflection to call "giveToPlayer"
        try {
             // com.cobblemon.mod.common.entity.pokemon.PokemonEntity
             // Method: getPokemon() returns Pokemon object
             // Pokemon object has giveToPlayer(Player)
             
             Object pokemonData = pokemon.getClass().getMethod("getPokemon").invoke(pokemon);
             pokemonData.getClass().getMethod("giveToPlayer", net.minecraft.entity.player.PlayerEntity.class).invoke(pokemonData, player);
             
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(Text.translatable("message.safari.capture_error", e.getMessage()).formatted(Formatting.RED), false);
        }
    }
}
