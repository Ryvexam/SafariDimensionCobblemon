package com.safari.events;

import com.safari.block.SafariBlocks;
import com.safari.config.SafariConfig;
import com.safari.session.SafariSessionManager;
import com.safari.world.SafariDimension;
import com.safari.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.Items;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.math.Direction;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.block.NetherPortalBlock;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;

public class SafariEvents {

    public static void init() {
        // 0. End battles in Safari immediately
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (isInSafari(player)) {
                    PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
                    if (battle != null && !battle.getEnded()) {
                        battle.end();
                    }
                }
            }
        });

        // 1. Setup Safari Entity Logic (Level scaling for Cobblemon) & Item Drops
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!world.getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) return;
            
            // Handle ItemEntity drops
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getStack();
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                // Replace Cobblemon safari ball with our custom one
                if ("cobblemon".equals(itemId.getNamespace()) && itemId.getPath().equals("safari_ball")) {
                    itemEntity.setStack(new ItemStack(ModItems.SAFARI_BALL, stack.getCount()));
                }
                return;
            }

            if (entity.isPlayer()) return;

            // Defer check
            if (world.getServer() != null) {
                world.getServer().execute(() -> {
                    Identifier id = Registries.ENTITY_TYPE.getId(entity.getType());
                    if (id.getNamespace().equals("cobblemon")) {
                        if (entity instanceof PokemonEntity pokemonEntity) {
                            if (!pokemonEntity.getCommandTags().contains("safari_level_set")) {
                                int min = Math.max(1, SafariConfig.get().safariMinLevel);
                                int max = Math.max(min, SafariConfig.get().safariMaxLevel);
                                int level = min + world.random.nextInt(max - min + 1);
                                pokemonEntity.getPokemon().setLevel(level);
                                pokemonEntity.addCommandTag("safari_level_set");
                            }
                        }
                    }
                });
            }
        });

        // 2. Block Breaking
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (isInSafari(player)) {
                if (!player.isCreative()) {
                    player.sendMessage(Text.translatable("message.safari.no_break_blocks").formatted(Formatting.RED), true);
                    return false;
                }
            }
            return true;
        });

        // 3. Block Placing + Portal Lighting
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            if (world.isClient) return ActionResult.PASS;

            // Portal lighting: Flint and Steel on frame
            if (!world.getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)
                    && player.getStackInHand(hand).isOf(Items.FLINT_AND_STEEL)
                    && world.getBlockState(pos).isOf(SafariBlocks.SAFARI_PORTAL_FRAME)) {

                if (tryLightPortal(world, pos)) {
                    player.getStackInHand(hand).damage(
                            1,
                            player,
                            hand == net.minecraft.util.Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND
                    );
                    return ActionResult.SUCCESS;
                }
            }

            // Block placing in safari
            if (isInSafari(player)) {
                if (!player.isCreative()) {
                    var stack = player.getStackInHand(hand);
                    if (stack != null && !stack.isEmpty()
                            && (stack.isOf(ModItems.SAFARI_NPC_SPAWN_EGG)
                            || stack.isOf(ModItems.SAFARI_PORTAL_NPC_SPAWN_EGG))) {
                        return ActionResult.PASS;
                    }
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        // 3b. Block non-safari balls in safari
        UseItemCallback.EVENT.register((player, world, hand) -> {
            var stack = player.getStackInHand(hand);
            if (!isInSafari(player)) {
                return net.minecraft.util.TypedActionResult.pass(stack);
            }
            if (stack == null || stack.isEmpty()) {
                return net.minecraft.util.TypedActionResult.pass(stack);
            }
            var itemId = Registries.ITEM.getId(stack.getItem());
            if (itemId == null) {
                return net.minecraft.util.TypedActionResult.pass(stack);
            }
            if ("safari".equals(itemId.getNamespace())) {
                return net.minecraft.util.TypedActionResult.pass(stack);
            }
            if ("cobblemon".equals(itemId.getNamespace()) && itemId.getPath().contains("ball")) {
                player.sendMessage(Text.translatable("message.safari.only_safari_balls").formatted(Formatting.RED), true);
                return net.minecraft.util.TypedActionResult.fail(stack);
            }
            return net.minecraft.util.TypedActionResult.pass(stack);
        });

        // 4. Logout Handling
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (SafariSessionManager.isInSession(handler.getPlayer())) {
                SafariSessionManager.pauseSession(handler.getPlayer());
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SafariSessionManager.resumeSession(handler.getPlayer());
        });

        // 5. Prevent attacking entities
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (isInSafari(player)) {
                if (entity instanceof BoatEntity || entity instanceof ChestBoatEntity) {
                    return ActionResult.PASS;
                }
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    private static boolean isInSafari(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return serverPlayer.getWorld().getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY);
        }
        return false;
    }

    private static boolean tryLightPortal(net.minecraft.world.World world, BlockPos clicked) {
        return tryLightPortalAxis(world, clicked, Direction.Axis.X) || tryLightPortalAxis(world, clicked, Direction.Axis.Z);
    }

    private static boolean tryLightPortalAxis(net.minecraft.world.World world, BlockPos clicked, Direction.Axis axis) {
        BlockPos origin = findFrameOrigin(world, clicked, axis);
        if (origin == null) return false;

        int width = measureFrameWidth(world, origin, axis);
        int height = measureFrameHeight(world, origin, axis);

        if (width < 4 || height < 5 || width > 23 || height > 23) return false; // Nether-like limits

        if (!isValidFrame(world, origin, axis, width, height)) return false;

        fillPortalInterior(world, origin, axis, width, height);
        return true;
    }

    private static BlockPos findFrameOrigin(net.minecraft.world.World world, BlockPos start, Direction.Axis axis) {
        if (!world.getBlockState(start).isOf(SafariBlocks.SAFARI_PORTAL_FRAME)) return null;

        BlockPos pos = start;
        int bottomY = world.getBottomY();

        // Move down to the bottom of the frame
        while (pos.getY() > bottomY && world.getBlockState(pos.down()).isOf(SafariBlocks.SAFARI_PORTAL_FRAME)) {
            pos = pos.down();
        }

        // Move to the left edge of the frame
        Direction negative = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        while (world.getBlockState(pos.offset(negative)).isOf(SafariBlocks.SAFARI_PORTAL_FRAME)) {
            pos = pos.offset(negative);
        }

        return pos;
    }

    private static int measureFrameWidth(net.minecraft.world.World world, BlockPos origin, Direction.Axis axis) {
        Direction positive = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        int width = 0;
        BlockPos pos = origin;
        while (width <= 23 && world.getBlockState(pos).isOf(SafariBlocks.SAFARI_PORTAL_FRAME)) {
            width++;
            pos = pos.offset(positive);
        }
        return width;
    }

    private static int measureFrameHeight(net.minecraft.world.World world, BlockPos origin, Direction.Axis axis) {
        int height = 0;
        BlockPos pos = origin;
        while (height <= 23 && world.getBlockState(pos).isOf(SafariBlocks.SAFARI_PORTAL_FRAME)) {
            height++;
            pos = pos.up();
        }
        return height;
    }

    private static boolean isValidFrame(net.minecraft.world.World world, BlockPos origin, Direction.Axis axis, int width, int height) {
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                boolean isEdge = w == 0 || w == width - 1 || h == 0 || h == height - 1;
                BlockPos check = axis == Direction.Axis.X
                        ? origin.add(w, h, 0)
                        : origin.add(0, h, w);

                if (isEdge) {
                    if (!world.getBlockState(check).isOf(SafariBlocks.SAFARI_PORTAL_FRAME)) return false;
                } else {
                    if (!world.getBlockState(check).isAir()) return false;
                }
            }
        }
        return true;
    }

    private static void fillPortalInterior(net.minecraft.world.World world, BlockPos origin, Direction.Axis axis, int width, int height) {
        for (int w = 1; w < width - 1; w++) {
            for (int h = 1; h < height - 1; h++) {
                BlockPos place = axis == Direction.Axis.X
                        ? origin.add(w, h, 0)
                        : origin.add(0, h, w);
                world.setBlockState(place, SafariBlocks.SAFARI_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, axis));
            }
        }
    }

    private static boolean isSafariNpcEntityId(Identifier id) {
        return "safari".equals(id.getNamespace())
                && ("safari_npc".equals(id.getPath()) || "safari_portal_npc".equals(id.getPath()));
    }
}
