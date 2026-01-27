package com.safari.economy;

import com.safari.SafariMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public interface EconomyProvider {
    BigDecimal getBalance(ServerPlayerEntity player);
    boolean hasEnough(ServerPlayerEntity player, BigDecimal amount);
    boolean deduct(ServerPlayerEntity player, BigDecimal amount);
    static void appendTransactionLog(ServerPlayerEntity player, String type, int amount, boolean success, int before, int after) {
        try {
            var server = player.getServer();
            if (server == null) return;
            Path logFile = server.getSavePath(WorldSavePath.ROOT).resolve("safari-transactions.log");
            String line = String.format(
                    "%s\t%s\t%s\t%s\tamount=%d\tsuccess=%s\tbalanceBefore=%d\tbalanceAfter=%d%n",
                    Instant.now().toString(),
                    player.getName().getString(),
                    player.getUuid(),
                    type,
                    amount,
                    success,
                    before,
                    after
            );
            Files.writeString(logFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            SafariMod.LOGGER.warn("Safari economy log write failed", e);
        }
    }
}