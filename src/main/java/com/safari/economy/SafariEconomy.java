package com.safari.economy;

import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;

public final class SafariEconomy {

    private SafariEconomy() {}

    public static BigDecimal getBalance(ServerPlayerEntity player) {
        return EconomyRegistry.get().getBalance(player);
    }

    public static boolean hasEnough(ServerPlayerEntity player, int amount) {
        return EconomyRegistry.get().hasEnough(player, BigDecimal.valueOf(amount));
    }

    public static boolean deduct(ServerPlayerEntity player, int amount) {
        return EconomyRegistry.get().deduct(player, BigDecimal.valueOf(amount));
    }
}