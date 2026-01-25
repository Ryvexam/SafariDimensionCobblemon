package com.safari.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.safari.config.SafariConfig;
import com.safari.session.SafariSession;
import com.safari.session.SafariSessionManager;
import com.safari.economy.SafariEconomy;
import com.safari.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SafariCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("safari")
            .executes(SafariCommand::info)
            .then(CommandManager.literal("enter").executes(SafariCommand::enter))
            .then(CommandManager.literal("leave").executes(SafariCommand::leave))
            .then(CommandManager.literal("info").executes(SafariCommand::info))
            .then(CommandManager.literal("buy")
                .then(CommandManager.literal("balls")
                    .then(CommandManager.literal("16").executes(ctx -> buyBalls(ctx, 16)))
                    .then(CommandManager.literal("32").executes(ctx -> buyBalls(ctx, 32)))
                    .then(CommandManager.literal("64").executes(ctx -> buyBalls(ctx, 64)))
                )
                .then(CommandManager.literal("time")
                    .then(CommandManager.argument("minutes", IntegerArgumentType.integer(1))
                        .executes(SafariCommand::buyTime)
                    )
                )
                .then(CommandManager.literal("ticket")
                    .then(CommandManager.argument("minutes", IntegerArgumentType.integer(1))
                        .executes(SafariCommand::buyTicket)
                    )
                )
            )
            // Admin commands
            .then(CommandManager.literal("reload").requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> {
                    SafariConfig.load();
                    ctx.getSource().sendMessage(Text.translatable("message.safari.config_reloaded").formatted(Formatting.GREEN));
                    return 1;
                })
            )
        );
    }


    private static int enter(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            return SafariSessionManager.tryStartSession(player, true) ? 1 : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int leave(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            if (!SafariSessionManager.isInSession(player)) {
                ctx.getSource().sendMessage(Text.translatable("message.safari.not_in_session").formatted(Formatting.RED));
                return 0;
            }
            SafariSessionManager.endSession(player);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int buyBalls(CommandContext<ServerCommandSource> ctx, int amount) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            if (!SafariSessionManager.isInSession(player)) {
                ctx.getSource().sendMessage(Text.translatable("message.safari.must_be_in_safari_supplies").formatted(Formatting.RED));
                return 0;
            }
            
            SafariSession session = SafariSessionManager.getSession(player);
            if (session.getPurchasedBalls() + amount > SafariConfig.get().maxBallsPurchasable) {
                ctx.getSource().sendMessage(Text.translatable("message.safari.purchase_limit").formatted(Formatting.RED));
                return 0;
            }

            int price = switch (amount) {
                case 16 -> SafariConfig.get().pack16BallsPrice;
                case 32 -> SafariConfig.get().pack32BallsPrice;
                case 64 -> SafariConfig.get().pack64BallsPrice;
                default -> 0;
            };
            
            if (price > 0 && SafariEconomy.deduct(player, price)) {
                com.safari.session.SafariInventoryHandler.giveSafariKit(player, amount);
                session.incrementPurchasedBalls(amount);
                ctx.getSource().sendMessage(Text.translatable("message.safari.purchased_balls", amount).formatted(Formatting.GREEN));
                return 1;
            } else {
                ctx.getSource().sendMessage(Text.translatable("message.safari.not_enough_money").formatted(Formatting.RED));
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private static int buyTime(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            if (!SafariSessionManager.isInSession(player)) {
                ctx.getSource().sendMessage(Text.translatable("message.safari.must_be_in_safari_time").formatted(Formatting.RED));
                return 0;
            }

            SafariSession session = SafariSessionManager.getSession(player);
            int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
            int minutesPerPack = Math.max(1, SafariConfig.get().timePurchaseMinutes);
            int pricePerPack = Math.max(0, SafariConfig.get().timePurchasePrice);
            int totalMinutes = minutes;
            int totalPrice = (int) Math.ceil((double) minutes * pricePerPack / minutesPerPack);

            if (SafariEconomy.deduct(player, totalPrice)) {
                long ticks = totalMinutes * 60L * 20L;
                session.addTime(ticks);
                ctx.getSource().sendMessage(Text.translatable("message.safari.added_time", totalMinutes).formatted(Formatting.GREEN));
                return 1;
            }

            ctx.getSource().sendMessage(Text.translatable("message.safari.need_money_time", totalPrice).formatted(Formatting.RED));
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int buyTicket(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            if (!SafariSessionManager.isInSession(player)) {
                ctx.getSource().sendMessage(Text.translatable("message.safari.must_be_in_safari_tickets").formatted(Formatting.RED));
                return 0;
            }

            int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
            Item ticket = getTicketItem(minutes);
            if (ticket == null) {
                ctx.getSource().sendMessage(Text.translatable("message.safari.invalid_ticket").formatted(Formatting.RED));
                return 0;
            }

            int minutesPerPack = Math.max(1, SafariConfig.get().timePurchaseMinutes);
            int pricePerPack = Math.max(0, SafariConfig.get().timePurchasePrice);
            int totalPrice = (int) Math.ceil((double) minutes * pricePerPack / minutesPerPack);

            if (!SafariEconomy.deduct(player, totalPrice)) {
                ctx.getSource().sendMessage(Text.translatable("message.safari.need_money_ticket", totalPrice).formatted(Formatting.RED));
                return 0;
            }

            var stack = new net.minecraft.item.ItemStack(ticket);
            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
            ctx.getSource().sendMessage(Text.translatable("message.safari.purchased_ticket", minutes).formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static Item getTicketItem(int minutes) {
        return switch (minutes) {
            case 5 -> ModItems.SAFARI_TICKET_5;
            case 15 -> ModItems.SAFARI_TICKET_15;
            case 30 -> ModItems.SAFARI_TICKET_30;
            default -> null;
        };
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            if (!SafariSessionManager.isInSession(player)) {
                ctx.getSource().sendMessage(Text.translatable("message.safari.not_in_session_info").formatted(Formatting.GRAY));
                return 0;
            }
            SafariSession session = SafariSessionManager.getSession(player);
            long minutes = (session.getTicksRemaining() / 20) / 60;
            ctx.getSource().sendMessage(Text.translatable("message.safari.session_info", minutes).formatted(Formatting.DARK_GREEN));
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}
