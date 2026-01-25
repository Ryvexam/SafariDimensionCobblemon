package com.safari.session;

import com.safari.config.SafariConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.File;
import java.io.IOException;

public class SafariInventoryHandler {

    private static File getInvFile(ServerPlayerEntity player) {
        // Saves in world/safari_inventories/uuid.dat
        java.nio.file.Path runDir = player.getServer().getRunDirectory();
        File dir = runDir.resolve("safari_inventories").toFile();
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, player.getUuidAsString() + ".dat");
    }

    public static void saveAndClear(ServerPlayerEntity player) {
        NbtCompound nbt = new NbtCompound();
        player.writeCustomDataToNbt(nbt); // Saves everything including inventory, xp, etc.
        
        // We only want to save Inventory-related NBT to restore later.
        NbtCompound invNbt = new NbtCompound();
        invNbt.put("Inventory", nbt.get("Inventory"));
        invNbt.put("XpP", nbt.get("XpP"));
        invNbt.put("XpLevel", nbt.get("XpLevel"));
        invNbt.put("XpTotal", nbt.get("XpTotal"));
        invNbt.put("foodLevel", nbt.get("foodLevel"));
        
        try {
            NbtIo.writeCompressed(invNbt, getInvFile(player).toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return; // Don't clear if save failed!
        }

        player.getInventory().clear();
        player.experienceLevel = 0;
        player.experienceProgress = 0;
        player.totalExperience = 0;
        player.getHungerManager().setFoodLevel(20);
    }

    public static void restore(ServerPlayerEntity player) {
        File file = getInvFile(player);
        if (!file.exists()) return;

        try {
            NbtCompound nbt = NbtIo.readCompressed(file.toPath(), net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
            
            // We read into a temporary NBT to avoid overwriting location data if we used readCustomDataFromNbt
            // But manually setting inventory is safer.
            NbtList invList = nbt.getList("Inventory", 10);
            player.getInventory().readNbt(invList);
            
            player.experienceLevel = nbt.getInt("XpLevel");
            player.experienceProgress = nbt.getFloat("XpP");
            player.totalExperience = nbt.getInt("XpTotal");
            player.getHungerManager().setFoodLevel(nbt.getInt("foodLevel"));
            
            file.delete(); // Delete after restore
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void giveSafariKit(ServerPlayerEntity player, int ballCount) {
        // Use our custom item
        String itemId = "safari:safari_ball"; 
        
        ItemStack balls = new ItemStack(Registries.ITEM.get(Identifier.of(itemId.split(":")[0], itemId.split(":")[1])));
        
        if (balls.getItem().toString().equals("minecraft:air")) {
             // Fallback
             balls = new ItemStack(Registries.ITEM.get(Identifier.of("cobblemon", "poke_ball")));
             player.sendMessage(Text.translatable("message.safari.ball_missing", itemId).formatted(Formatting.RED), false);
        }
        
        balls.setCount(ballCount);
        player.getInventory().insertStack(balls);
    }

    public static void removeSafariBalls(ServerPlayerEntity player) {
        Identifier safariBallId = Identifier.of("safari", "safari_ball");

        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            Identifier stackId = Registries.ITEM.getId(stack.getItem());
            if (safariBallId.equals(stackId)) {
                inv.setStack(i, ItemStack.EMPTY);
            }
        }
    }
}
