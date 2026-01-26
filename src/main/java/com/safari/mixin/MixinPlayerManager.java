package com.safari.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {
    @Inject(method = "addWorldBorderListener", at = @At("HEAD"), cancellable = true)
    private void onAddWorldBorderListener(ServerWorld world, CallbackInfo ci) {
        // Only cancel vanilla's global world border listener for the Safari dimension.
        // We use string check to avoid early class loading of mod classes.
        if (world != null && world.getRegistryKey().getValue().getPath().equals("safari")) {
            ci.cancel();
        }
    }
}
