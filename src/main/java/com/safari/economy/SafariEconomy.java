package com.safari.economy;

import net.minecraft.server.network.ServerPlayerEntity;
import java.lang.reflect.Method;
import java.math.BigDecimal;

public class SafariEconomy {

    public static boolean hasEnough(ServerPlayerEntity player, int amount) {
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
            
            return current.compareTo(BigDecimal.valueOf(amount)) >= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deduct(ServerPlayerEntity player, int amount) {
        try {
            Object manager = getManager();
            if (manager == null) return false;

            // public boolean subtractBalance(UUID uuid, BigDecimal amount)
            Method subBal = manager.getClass().getMethod("subtractBalance", java.util.UUID.class, BigDecimal.class);
            return (boolean) subBal.invoke(manager, player.getUuid(), BigDecimal.valueOf(amount));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int getBalance(ServerPlayerEntity player) {
        try {
            Object manager = getManager();
            if (manager == null) return 0;

            Method getBal = manager.getClass().getMethod("getBalance", java.util.UUID.class);
            BigDecimal current = (BigDecimal) getBal.invoke(manager, player.getUuid());
            if (current == null) return 0;
            return current.intValue();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    private static Object getManager() {
        try {
            // public static EconomyManager getEconomyManager()
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
