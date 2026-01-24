package com.safari.item;

import com.safari.session.SafariSession;
import com.safari.session.SafariSessionManager;
import com.safari.world.SafariDimension;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class SafariTimeTicketItem extends Item {
    private final int minutes;

    public SafariTimeTicketItem(int minutes, Settings settings) {
        super(settings);
        this.minutes = minutes;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.fail(stack);
        }

        if (!player.getWorld().getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) {
            player.sendMessage(Text.of("§cYou can only use this in the Safari Zone."), false);
            return TypedActionResult.fail(stack);
        }

        SafariSession session = SafariSessionManager.getSession(player);
        if (session == null) {
            player.sendMessage(Text.of("§cYou are not in a Safari session."), false);
            return TypedActionResult.fail(stack);
        }

        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        session.addTime(minutes * 60L * 20L);
        stack.decrement(1);
        player.getItemCooldownManager().set(this, 20);
        player.sendMessage(Text.of("§aAdded " + minutes + " minutes to your Safari session."), false);
        return TypedActionResult.success(stack);
    }
}
