package com.github.cerealklla.kyt.system;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.github.cerealklla.kyt.loadout.LoadoutRecord;
import com.github.cerealklla.kyt.loadout.LoadoutStorage;
import com.github.cerealklla.kyt.welcome.WelcomeBookContent;

import net.minecraft.world.item.ItemStack;

/**
 * Builds and saves Kyt's two mod-managed ({@code systemManaged=true}) Kyts fresh on every real
 * server start -- "Dev" (replaces the old unconditional-on-login debug item grants that used to
 * live in Lyfe/Settlemynts/Blueprynts, see each mod's own decisions.md 2026-10-02 entry) and
 * "Welcome" (a one-time first-join grant, see {@code welcome.WelcomeListener}). Rewriting both
 * every boot means "Welcome"'s book always reflects currently-installed mods and "Dev"'s contents
 * always match current code -- {@code LoadoutStorage#save} already deletes-then-rewrites, so this
 * is the same idiom every other save already uses, just run unconditionally at boot.
 *
 * <p>Bootstrapped from {@code ServerStartingEvent} (NeoForge event bus), not {@code
 * FMLCommonSetupEvent} -- deliberately later than this mod's own registration-adjacent setup,
 * because {@link #bootstrap()} must run only after every sibling mod's constructor has had a
 * chance to call {@link #registerDevKytContribution}; {@code ServerStartingEvent} is the first
 * point guaranteed to come after every mod's construction across the whole JVM, regardless of
 * inter-mod load order.
 */
public final class SystemKyts {

    public static final String DEV_NAME = "Dev";
    public static final String WELCOME_NAME = "Welcome";

    private SystemKyts() {
    }

    public static void registerDevKytContribution(Supplier<List<ItemStack>> items) {
        DevKytContributions.register(items);
    }

    public static void bootstrap() {
        LoadoutStorage.get().save(buildDevKyt());
        LoadoutStorage.get().save(buildWelcomeKyt());
    }

    /**
     * Kyt's own vanilla-item base in the hotbar, plus every registered sibling-mod contribution
     * (Lyfe's crafting-overhaul blocks, Settlemynts' Settlement Claim Flag, Blueprynts'
     * Construction Site item, Caryvan's Hand Cart, whichever of those mods are actually loaded)
     * flattened into the {@code inventory} overflow -- see {@code LoadoutRecord}'s own doc for why
     * that field exists.
     *
     * <p><b>Hotbar emptied, 2026-10-25</b> (explicit user request, testing Caryvan's Phase 1 Hand
     * Cart) -- the old fixed hotbar grab-bag (Flint and Steel, Planks, Stone Sword, Cobblestone,
     * Stick, Sign, Map, Fence -- general-purpose testing items from well before this mod's own
     * contribution system existed) is gone outright rather than fought with: whatever's currently
     * useful to have immediately in-hand belongs to whichever mod actually owns that feature,
     * contributed through the normal {@link #registerDevKytContribution} path into the
     * {@code inventory} overflow below, same as everything else. If a genuinely new general-purpose
     * testing item is needed again later, re-add it here deliberately rather than reviving this list
     * wholesale.
     */
    private static LoadoutRecord buildDevKyt() {
        List<ItemStack> hotbar = new ArrayList<>();

        List<ItemStack> inventory = new ArrayList<>(DevKytContributions.collectAll());

        return new LoadoutRecord(DEV_NAME, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, hotbar, true, inventory);
    }

    private static LoadoutRecord buildWelcomeKyt() {
        List<ItemStack> hotbar = new ArrayList<>();
        hotbar.add(WelcomeBookContent.build());
        return new LoadoutRecord(WELCOME_NAME, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, hotbar, true, LoadoutRecord.emptyInventory());
    }
}
