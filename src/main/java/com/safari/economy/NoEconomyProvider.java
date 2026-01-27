package com.safari.economy;

import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;

public final class NoEconomyProvider implements EconomyProvider {
    public static final NoEconomyProvider INSTANCE = new NoEconomyProvider();
    private NoEconomyProvider() {}

    @Override public BigDecimal getBalance(ServerPlayerEntity player) { return BigDecimal.ZERO; }
    @Override public boolean hasEnough(ServerPlayerEntity player, BigDecimal amount) { return false; }
    @Override public boolean deduct(ServerPlayerEntity player, BigDecimal amount) { return false; }
}