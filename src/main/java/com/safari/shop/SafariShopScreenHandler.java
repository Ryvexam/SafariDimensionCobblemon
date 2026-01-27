package com.safari.shop;

import com.safari.config.SafariConfig;
import com.safari.economy.SafariEconomy;
import com.safari.item.ModItems;
import com.safari.session.SafariSessionManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.LinkedHashMap;
import java.util.Map;

public class SafariShopScreenHandler extends ScreenHandler {
    private static final int SHOP_ROWS = 3;
    private static final int SHOP_COLUMNS = 9;
    private static final int SHOP_SIZE = SHOP_ROWS * SHOP_COLUMNS;

    private final PlayerEntity player;
    private final ShopInventory inventory;
    private final Map<Integer, ShopItem> shopItems = new LinkedHashMap<>();

    public SafariShopScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        this.player = playerInventory.player;

        this.inventory = new ShopInventory(SHOP_SIZE);
        addShopItem(inventory, 10, ModItems.SAFARI_BALL, 16);
        addShopItem(inventory, 11, ModItems.SAFARI_BALL, 32);
        addShopItem(inventory, 12, ModItems.SAFARI_BALL, 64);
        addShopItem(inventory, 14, ModItems.SAFARI_TICKET_5, 1);
        addShopItem(inventory, 15, ModItems.SAFARI_TICKET_15, 1);
        addShopItem(inventory, 16, ModItems.SAFARI_TICKET_30, 1);
        updateBalanceDisplay();

        for (int i = 0; i < SHOP_SIZE; i++) {
            addSlot(new ShopSlot(inventory, i, 8 + (i % SHOP_COLUMNS) * 18, 18 + (i / SHOP_COLUMNS) * 18));
        }

        int inventoryY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, inventoryY + row * 18));
            }
        }

        int hotbarY = 142;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    private void addShopItem(ShopInventory inventory, int slot, Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.setCount(count);
        int price = getPrice(new ShopItem(item, count));
        if (price > 0) {
            java.util.List<Text> lore = new java.util.ArrayList<>();
            lore.add(Text.translatable("message.safari.price", price).formatted(Formatting.GRAY));
            stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(lore));
        }
        inventory.setStack(slot, stack);
        shopItems.put(slot, new ShopItem(item, count));
    }

    @Override
    public void onSlotClick(int slotId, int button, net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity player) {
        if (slotId >= 0 && slotId < SHOP_SIZE && shopItems.containsKey(slotId)) {
            handlePurchase(shopItems.get(slotId));
            return;
        }
        super.onSlotClick(slotId, button, actionType, player);
    }

    private void handlePurchase(ShopItem item) {
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
            return;
        }

        if (!SafariSessionManager.isInSession(serverPlayer)) {
            player.sendMessage(Text.translatable("message.safari.must_be_in_session_buy").formatted(Formatting.RED), false);
            return;
        }

        int price = getPrice(item);
        if (!SafariEconomy.deduct(serverPlayer, price)) {
            player.sendMessage(Text.translatable("message.safari.need_money_buy", price).formatted(Formatting.RED), false);
            return;
        }

        ItemStack stack = new ItemStack(item.item(), item.count());
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
        updateShopPrices();
        updateBalanceDisplay();
    }

    private void updateShopPrices() {
        for (Map.Entry<Integer, ShopItem> entry : shopItems.entrySet()) {
            ItemStack stack = inventory.getStack(entry.getKey()).copy();
            int price = getPrice(entry.getValue());
            if (price > 0) {
                java.util.List<Text> lore = new java.util.ArrayList<>();
                lore.add(Text.translatable("message.safari.price", price).formatted(Formatting.GRAY));
                stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(lore));
            }
            inventory.setStack(entry.getKey(), stack);
        }
        sendContentUpdates();
    }

    private void updateBalanceDisplay() {
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
            return;
        }

        int balance = SafariEconomy.getBalance(serverPlayer).intValue();
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("message.safari.balance", balance).formatted(Formatting.GREEN));

        ProfileComponent component = new ProfileComponent(serverPlayer.getGameProfile());
        head.set(DataComponentTypes.PROFILE, component);

        inventory.setStack(4, head);
        sendContentUpdates();
    }

    private int getPrice(ShopItem item) {
        if (item.item() == ModItems.SAFARI_BALL) {
            int pack16 = SafariConfig.get().pack16BallsPrice;
            int pack32 = SafariConfig.get().pack32BallsPrice;
            int pack64 = SafariConfig.get().pack64BallsPrice;
            if (item.count() == 16) {
                return pack16;
            }
            if (item.count() == 32) {
                return pack32;
            }
            if (item.count() == 64) {
                return pack64;
            }
            return 0;
        }

        int minutesPerPack = Math.max(1, SafariConfig.get().timePurchaseMinutes);
        int pricePerPack = Math.max(0, SafariConfig.get().timePurchasePrice);
        int minutes = 0;
        if (item.item() == ModItems.SAFARI_TICKET_5) {
            minutes = 5;
        } else if (item.item() == ModItems.SAFARI_TICKET_15) {
            minutes = 15;
        } else if (item.item() == ModItems.SAFARI_TICKET_30) {
            minutes = 30;
        }
        return (int) Math.ceil((double) minutes * pricePerPack / minutesPerPack);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    private static final class ShopItem {
        private final Item item;
        private final int count;

        private ShopItem(Item item, int count) {
            this.item = item;
            this.count = count;
        }

        private Item item() {
            return item;
        }

        private int count() {
            return count;
        }
    }

    private static final class ShopSlot extends Slot {
        private ShopSlot(ShopInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false;
        }
    }
}
