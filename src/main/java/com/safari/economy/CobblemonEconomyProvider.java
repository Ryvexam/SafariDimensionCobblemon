package com.safari.economy;

import com.safari.SafariMod;
import net.minecraft.server.network.ServerPlayerEntity;
import java.lang.reflect.Method;
import java.math.BigDecimal;

public class CobblemonEconomyProvider implements EconomyProvider {

    public boolean hasEnough(ServerPlayerEntity player, BigDecimal amount) {
        try {
            Object manager = getManager();
            if (manager == null) {
                System.err.println("SafariEconomy: EconomyManager is null");
                return false;
            }

            // public BigDecimal getBalance(UUID uuid)
            Method getBal = manager.getClass().getMethod("getBalance", java.util.UUID.class);
            BigDecimal current = (BigDecimal) getBal.invoke(manager, player.getUuid());

            if (current == null) return false;

            return current.compareTo(amount) >= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deduct(ServerPlayerEntity player, BigDecimal amount) {
        try {
            Object manager = getManager();
            if (manager == null) return false;

            Method subBal = manager.getClass().getMethod("subtractBalance", java.util.UUID.class, BigDecimal.class);
            BigDecimal before = getBalance(player);
            boolean success = (boolean) subBal.invoke(manager, player.getUuid(), amount);
            BigDecimal after = getBalance(player);
            SafariMod.LOGGER.info(
                    "Safari economy deduct: player={}, amount={}, success={}, balanceBefore={}, balanceAfter={}",
                    player.getName().getString(),
                    amount,
                    success,
                    before,
                    after
            );
            EconomyProvider.appendTransactionLog(player, "deduct", amount.intValue(), success, before.intValue(), after.intValue());
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public BigDecimal getBalance(ServerPlayerEntity player) {
        try {
            Object manager = getManager();
            if (manager == null) return new BigDecimal(0);

            Method getBal = manager.getClass().getMethod("getBalance", java.util.UUID.class);
            BigDecimal current = (BigDecimal) getBal.invoke(manager, player.getUuid());
            if (current == null) return new BigDecimal(0);
            return current;
        } catch (Exception e) {
            e.printStackTrace();
            return new BigDecimal(0);
        }
    }

    private static Object getManager() {
        try {
            Class<?> mainClass = Class.forName("com.cobblemon.economy.fabric.CobblemonEconomy");
            Method getMgr = mainClass.getMethod("getEconomyManager");
            return getMgr.invoke(null);
        } catch (Exception e) {
            System.err.println("SafariEconomy: Failed to get EconomyManager via Reflection");
            e.printStackTrace();
            return null;
        }
    }
}