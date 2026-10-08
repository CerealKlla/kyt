package com.github.cerealklla.kyt.loadout;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Grants a saved {@link LoadoutRecord}'s contents directly to a real player's own inventory --
 * distinct from {@code Settlemynts' GuardEntity#equipFromLoadout}, which force-sets a freshly
 * spawned guard's equipment slots (safe, since a guard always starts empty). A live player may
 * already be wearing/carrying things, so this is purely additive: every non-empty stack across
 * armor/offhand/hotbar/inventory is added to the player's own inventory, never force-equipped,
 * with the same "drop at their feet if the inventory is full" fallback already used by {@code
 * Lyfe's knowledge.SignListener} for its map-creation code.
 */
public final class LoadoutApplier {

    private LoadoutApplier() {
    }

    public static boolean grantToPlayer(ServerPlayer player, LoadoutRecord record) {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(record.helmet());
        stacks.add(record.chestplate());
        stacks.add(record.leggings());
        stacks.add(record.boots());
        stacks.add(record.offhand());
        stacks.addAll(record.hotbar());
        stacks.addAll(record.inventory());

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }
        return true;
    }
}
