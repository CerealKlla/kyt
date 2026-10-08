package com.github.cerealklla.kyt.debug;

import com.mojang.brigadier.CommandDispatcher;

import com.github.cerealklla.kyt.api.Kyt;
import com.github.cerealklla.kyt.system.SystemKyts;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * DEBUG ONLY -- deliberately no permission gate, same precedent as Lyfe's/Settlemynts'/Blueprynts'
 * own {@code debug.DebugCommands} (local-dev {@code ops.json} friction, no untrusted players on
 * this server).
 */
public final class DebugCommands {

    private DebugCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kyt")
                .requires(source -> true) // deliberately no permission gate -- see class doc
                .then(Commands.literal("getDev")
                        .executes(ctx -> getDev(ctx.getSource()))));
    }

    private static int getDev(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only a player can run this."));
            return 0;
        }
        boolean granted = Kyt.grantToPlayer(player, SystemKyts.DEV_NAME);
        if (granted) {
            source.sendSuccess(() -> Component.literal("Granted the Dev Kyt."), false);
            return 1;
        }
        source.sendFailure(Component.literal("Dev Kyt not found -- this should never happen."));
        return 0;
    }
}
