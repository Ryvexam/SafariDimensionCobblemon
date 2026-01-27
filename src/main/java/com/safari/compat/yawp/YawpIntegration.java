package com.safari.compat.yawp;

import de.z0rdak.yawp.api.Flag;
import de.z0rdak.yawp.api.FlagEvaluator;
import de.z0rdak.yawp.api.FlagRegister;
import de.z0rdak.yawp.api.events.flag.FlagCheckRequest;
import de.z0rdak.yawp.core.flag.FlagFrequency;
import de.z0rdak.yawp.core.flag.FlagMetaInfo;
import de.z0rdak.yawp.core.flag.FlagState;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Set;

public class YawpIntegration {
    public static final String FLAG_ID = "melee-npc-safari";
    public static Flag SAFARI_FLAG;

    public static void register() {
        // Registering a flag named "melee-npc-safari" under the "yawp" namespace
        FlagMetaInfo meta = new FlagMetaInfo(Set.of(), FlagFrequency.NORMAL);
        FlagRegister.registerFlag("yawp", FLAG_ID, meta);
        SAFARI_FLAG = FlagRegister.getFlag(Identifier.of("yawp", FLAG_ID));
    }

    public static Boolean checkFlag(Entity target, Entity attacker) {
        if (SAFARI_FLAG == null) return null;
        
        if (!(target.getWorld() instanceof ServerWorld world)) return null;
        
        PlayerEntity player = (attacker instanceof PlayerEntity p) ? p : null;
        
        // Evaluate the flag at the target's position
        FlagCheckRequest request = new FlagCheckRequest(
            target.getBlockPos(), 
            null, 
            world.getRegistryKey(), 
            player,
            SAFARI_FLAG.id().toString() 
        );
        
        FlagState state = FlagEvaluator.evaluate(request).getFlagState();
        if (state == FlagState.ALLOWED) return true;
        if (state == FlagState.DENIED) return false;
        return null;
    }
}
