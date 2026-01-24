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
                    ctx.getSource().sendMessage(Text.of("§aConfig reloaded!"));
                    return 1;
                })
            )
        );
    }


    private static int enter(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            if (SafariSessionManager.isInSession(player)) {
                ctx.getSource().sendMessage(Text.of("§cYou are already in a Safari session!"));
                return 0;
            }

            // Check if player has free inventory slot for Safari Balls
            if (player.getInventory().getEmptySlot() == -1) {
                ctx.getSource().sendMessage(Text.of("§cYour inventory is full! You need 1 slot for Safari Balls before entering Safari."));
                return 0;
            }

            int price = SafariConfig.get().entrancePrice;
            if (!SafariEconomy.deduct(player, price)) {
                ctx.getSource().sendMessage(Text.of("§cYou need " + price + " Pokédollars to enter!"));
                return 0;
            }
            
            ctx.getSource().sendMessage(Text.of("§aPaid " + price + " Pokédollars. Entering Safari..."));
            SafariSessionManager.startSession(player);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int leave(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            if (!SafariSessionManager.isInSession(player)) {
                ctx.getSource().sendMessage(Text.of("§cYou are not in a Safari session!"));
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
                ctx.getSource().sendMessage(Text.of("§cYou must be in the Safari to buy supplies!"));
                return 0;
            }
            
            SafariSession session = SafariSessionManager.getSession(player);
            if (session.getPurchasedBalls() + amount > SafariConfig.get().maxBallsPurchasable) {
                ctx.getSource().sendMessage(Text.of("§cYou have reached the purchase limit for this session!"));
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
                ctx.getSource().sendMessage(Text.of("§aPurchased " + amount + " Safari Balls!"));
                return 1;
            } else {
                ctx.getSource().sendMessage(Text.of("§cNot enough money!"));
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
                ctx.getSource().sendMessage(Text.of("§cYou must be in the Safari to buy time!"));
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
                ctx.getSource().sendMessage(Text.of("§aAdded " + totalMinutes + " minutes to your Safari session."));
                return 1;
            }

            ctx.getSource().sendMessage(Text.of("§cYou need " + totalPrice + " Pokédollars to buy time!"));
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int buyTicket(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            if (!SafariSessionManager.isInSession(player)) {
                ctx.getSource().sendMessage(Text.of("§cYou must be in the Safari to buy tickets!"));
                return 0;
            }

            int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
            Item ticket = getTicketItem(minutes);
            if (ticket == null) {
                ctx.getSource().sendMessage(Text.of("§cInvalid ticket. Use 5, 15, or 30."));
                return 0;
            }

            int minutesPerPack = Math.max(1, SafariConfig.get().timePurchaseMinutes);
            int pricePerPack = Math.max(0, SafariConfig.get().timePurchasePrice);
            int totalPrice = (int) Math.ceil((double) minutes * pricePerPack / minutesPerPack);

            if (!SafariEconomy.deduct(player, totalPrice)) {
                ctx.getSource().sendMessage(Text.of("§cYou need " + totalPrice + " Pokédollars to buy this ticket!"));
                return 0;
            }

            var stack = new net.minecraft.item.ItemStack(ticket);
            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
            ctx.getSource().sendMessage(Text.of("§aPurchased a +" + minutes + "m Safari ticket."));
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
                ctx.getSource().sendMessage(Text.of("§7You are not currently in a Safari session."));
                return 0;
            }
            SafariSession session = SafariSessionManager.getSession(player);
            long minutes = (session.getTicksRemaining() / 20) / 60;
            ctx.getSource().sendMessage(Text.of("§2Safari Session Info:\n§7Time Left: " + minutes + "m"));
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}
