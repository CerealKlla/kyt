package com.github.cerealklla.kyt.system;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.github.cerealklla.kyt.loadout.LoadoutRecord;
import com.github.cerealklla.kyt.loadout.LoadoutStorage;
import com.github.cerealklla.kyt.welcome.WelcomeBookContent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
     * Construction Site item, whichever of those mods are actually loaded) flattened into the
     * {@code inventory} overflow -- see {@code LoadoutRecord}'s own doc for why that field exists.
     *
     * <p><b>Flint and Steel, not an enchanted sword</b> (confirmed live 2026-10-02) -- the deleted
     * {@code LyfeMod#onPlayerLoggedIn} originally granted a Fire Aspect-enchanted Iron Sword for
     * testing Survivalist/Cook's burn-kill XP trigger. {@code LoadoutStorage#save} encodes via
     * plain {@code JsonOps}, which has no registry access -- an enchanted {@code ItemStack} fails
     * to encode ("Can't access registry... minecraft:enchantment"), confirmed by a real crash on
     * first boot. This is a genuine limitation of {@code LoadoutStorage}'s storage layer (it was
     * never exercised before, since the Kyt Editor's item palette only ever produces plain,
     * unenchanted items) -- fixing that properly would mean threading a live {@code RegistryAccess}
     * through every save/load call site, out of scope for this dev-only convenience. Flint and
     * Steel achieves the exact same testing goal (lighting something on fire) with no enchantment
     * involved.
     */
    private static LoadoutRecord buildDevKyt() {
        List<ItemStack> hotbar = new ArrayList<>();
        hotbar.add(new ItemStack(Items.FLINT_AND_STEEL));
        hotbar.add(new ItemStack(Items.OAK_PLANKS, 16));
        hotbar.add(new ItemStack(Items.STONE_SWORD));
        hotbar.add(new ItemStack(Items.COBBLESTONE, 16));
        hotbar.add(new ItemStack(Items.STICK, 16));
        hotbar.add(new ItemStack(Items.OAK_SIGN, 16));
        hotbar.add(new ItemStack(Items.MAP, 8));
        hotbar.add(new ItemStack(Items.OAK_FENCE, 16));

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
