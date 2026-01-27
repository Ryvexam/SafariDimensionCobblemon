package com.safari.economy;

import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public final class EconomyRegistry {
    private static final AtomicReference<EconomyProvider> PROVIDER = new AtomicReference<>();

    private EconomyRegistry() {}

    public static void register(EconomyProvider provider) {
        register(provider, 0);
    }

    public static synchronized void register(EconomyProvider provider, int priority) {
        EconomyProvider current = PROVIDER.get();

        if (current == null) {
            PROVIDER.set(new PrioritizedProvider(provider, priority));
            return;
        }

        PrioritizedProvider curr = (PrioritizedProvider) current;
        if (priority > curr.priority) {
            PROVIDER.set(new PrioritizedProvider(provider, priority));
        }
    }

    public static EconomyProvider get() {
        EconomyProvider p = PROVIDER.get();
        return p != null ? ((PrioritizedProvider)p).provider : NoEconomyProvider.INSTANCE;
    }

    // Internal wrapper
    private record PrioritizedProvider(EconomyProvider provider, int priority) implements EconomyProvider {

        @Override
        public BigDecimal getBalance(ServerPlayerEntity player) {
                return provider.getBalance(player);
            }

        @Override
        public boolean hasEnough(ServerPlayerEntity player, BigDecimal amount) {
                return provider.hasEnough(player, amount);
            }

        @Override
        public boolean deduct(ServerPlayerEntity player, BigDecimal amount) {
                return provider.deduct(player, amount);
            }
    }
}