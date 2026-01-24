package com.safari.mixin;

import com.safari.world.SafariDimension;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityDamage {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void safari$blockDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity player
                && player.getWorld().getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) {
            cir.setReturnValue(false);
            return;
        }

        if (source.getAttacker() instanceof ServerPlayerEntity attacker
                && attacker.getWorld().getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) {
            cir.setReturnValue(false);
        }
    }
}
