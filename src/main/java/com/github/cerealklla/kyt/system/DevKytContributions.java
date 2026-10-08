package com.github.cerealklla.kyt.system;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;

/**
 * In-memory registry of sibling mods' own contributions to the mod-managed "Dev" Kyt -- Kyt itself
 * deliberately has zero sibling-mod dependencies ("Kyt stands alone," see CLAUDE.md), so it can't
 * directly reference e.g. Lyfe's Research Bench or Settlemynts' Settlement Claim Flag. Instead,
 * each sibling mod calls {@code api.Kyt#registerDevKytContribution} (which delegates here) from
 * its own constructor, gated behind {@code ModList.get().isLoaded("kyt")} -- the dependency only
 * ever points from the sibling mod toward Kyt, never the reverse.
 *
 * <p>Registration order across mods is never relied on: contributions are only ever *collected*
 * (by {@link SystemKyts#bootstrap()}), which runs at {@code ServerStartingEvent} -- guaranteed to
 * fire after every mod's constructor (where registration happens) across the whole JVM, regardless
 * of inter-mod load order.
 */
final class DevKytContributions {

    private static final List<Supplier<List<ItemStack>>> CONTRIBUTIONS = new CopyOnWriteArrayList<>();

    private DevKytContributions() {
    }

    static void register(Supplier<List<ItemStack>> items) {
        CONTRIBUTIONS.add(items);
    }

    static List<ItemStack> collectAll() {
        List<ItemStack> all = new ArrayList<>();
        for (Supplier<List<ItemStack>> contribution : CONTRIBUTIONS) {
            all.addAll(contribution.get());
        }
        return all;
    }
}
