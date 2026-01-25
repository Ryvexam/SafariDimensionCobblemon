package com.safari.mixin;

import com.safari.session.SafariSessionManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerDeath {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void safari$endSessionOnDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (SafariSessionManager.isInSession(player)) {
            SafariSessionManager.endSessionOnDeath(player);
        }
    }
}
