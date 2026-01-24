package com.safari.item;

import com.safari.SafariMod;
import com.safari.block.SafariBlocks;
import com.safari.entity.SafariEntities;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item SAFARI_BALL = register("safari_ball", new SafariBallItem(new Item.Settings().maxCount(64)));
    public static final Item SAFARI_TICKET_5 = register("ticket_5", new SafariTimeTicketItem(5, new Item.Settings().maxCount(16)));
    public static final Item SAFARI_TICKET_15 = register("ticket_15", new SafariTimeTicketItem(15, new Item.Settings().maxCount(16)));
    public static final Item SAFARI_TICKET_30 = register("ticket_30", new SafariTimeTicketItem(30, new Item.Settings().maxCount(16)));
    public static final Item SAFARI_NPC_SPAWN_EGG = register(
            "safari_npc_spawn_egg",
            new SpawnEggItem(SafariEntities.SAFARI_NPC, 0xE5D1B8, 0x4B7A3C, new Item.Settings())
    );

    public static final RegistryKey<ItemGroup> SAFARI_ITEM_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(SafariMod.MOD_ID, "item_group"));
    public static final ItemGroup SAFARI_ITEM_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(SAFARI_BALL))
            .displayName(Text.translatable("itemGroup.safari.item_group"))
            .entries((context, entries) -> {
                entries.add(SAFARI_BALL);
                entries.add(SAFARI_TICKET_5);
                entries.add(SAFARI_TICKET_15);
                entries.add(SAFARI_TICKET_30);
                entries.add(SAFARI_NPC_SPAWN_EGG);
                entries.add(SafariBlocks.SAFARI_PORTAL_FRAME);
            })
            .build();

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(SafariMod.MOD_ID, name), item);
    }

    public static void registerModItems() {
        SafariMod.LOGGER.info("Registering Mod Items for " + SafariMod.MOD_ID);
        Registry.register(Registries.ITEM_GROUP, SAFARI_ITEM_GROUP_KEY, SAFARI_ITEM_GROUP);
    }
}
