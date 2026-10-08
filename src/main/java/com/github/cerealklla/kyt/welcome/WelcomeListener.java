package com.github.cerealklla.kyt.welcome;

import com.github.cerealklla.kyt.api.Kyt;
import com.github.cerealklla.kyt.registration.ModAttachments;
import com.github.cerealklla.kyt.system.SystemKyts;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Grants the mod-managed "Welcome" Kyt exactly once per player -- their true first login ever,
 * never again on a later login or after death (see {@code ModAttachments#WELCOME_GRANT_STATE}'s
 * own {@code copyOnDeath()} note for why death specifically needed care).
 */
public final class WelcomeListener {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        WelcomeGrantState state = player.getData(ModAttachments.WELCOME_GRANT_STATE);
        if (state.granted()) {
            return;
        }
        Kyt.grantToPlayer(serverPlayer, SystemKyts.WELCOME_NAME);
        player.setData(ModAttachments.WELCOME_GRANT_STATE, new WelcomeGrantState(true));
    }
}
