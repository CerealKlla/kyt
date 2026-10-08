package com.github.cerealklla.kyt.api;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.cerealklla.kyt.loadout.LoadoutApplier;
import com.github.cerealklla.kyt.loadout.LoadoutRecord;
import com.github.cerealklla.kyt.loadout.LoadoutStorage;
import com.github.cerealklla.kyt.system.SystemKyts;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The stable public entry point for other mods to integrate with Kyt -- read-only lookups against
 * the saved Kyt loadout store. Same "stable facade, don't reach into internals" pattern as
 * Blueprynts' {@code api.Blueprynts}, Cartographyr's {@code Cartography}, Settlemynts' {@code
 * api.Settlemynts}. Kyt's own GUI writes directly via {@code loadout.LoadoutStorage}, not through
 * this facade -- no {@code saveKyt} is exposed here.
 */
public final class Kyt {

    private Kyt() {
    }

    public static Optional<LoadoutRecord> getKyt(String name) {
        return LoadoutStorage.get().load(name);
    }

    /**
     * Every saved Kyt name, excluding any {@link LoadoutRecord#systemManaged()} entry (added
     * 2026-10-02, see decisions.md) -- mod-managed Kyts like "Dev"/"Welcome" (see {@code
     * system.SystemKyts}) must never appear in the Kyt Editor's own picker or Settlemynts'
     * Garrison "Select Kyt" picker, both of which call this method. Re-decodes each name's record
     * to check the flag -- a second decode pass beyond {@link LoadoutStorage#listNames()}'s own,
     * accepted since these are small files and this isn't a hot path; keeps {@code LoadoutStorage}
     * itself generic/storage-only rather than pushing this business rule down into it.
     */
    public static List<String> listKytNames() {
        return LoadoutStorage.get().listNames().stream()
                .filter(name -> !getKyt(name).map(LoadoutRecord::systemManaged).orElse(false))
                .toList();
    }

    public static boolean kytExists(String name) {
        return getKyt(name).isPresent();
    }

    /**
     * Grants {@code kytName}'s saved contents directly to {@code player}'s own inventory (added
     * 2026-10-02, see decisions.md) -- purely additive, never force-equips (see {@code
     * loadout.LoadoutApplier}'s own doc for why this differs from Settlemynts' {@code
     * GuardEntity#equipFromLoadout}). Used by Kyt's own {@code /kyt getDev} command and the
     * first-join Welcome grant ({@code welcome.WelcomeListener}) -- intentionally not restricted to
     * {@code systemManaged} Kyts, so a future caller could grant any Kyt to a player, not just the
     * two built-in ones.
     *
     * @return false if no Kyt named {@code kytName} exists; true otherwise (even if it happened to
     * be empty -- an empty managed Kyt isn't a failure).
     */
    public static boolean grantToPlayer(ServerPlayer player, String kytName) {
        return getKyt(kytName).map(record -> LoadoutApplier.grantToPlayer(player, record)).orElse(false);
    }

    /**
     * The integration point for a sibling mod (Lyfe, Settlemynts, Blueprynts) to contribute its
     * own custom debug items into Kyt's mod-managed "Dev" Kyt (added 2026-10-02, see decisions.md)
     * -- Kyt itself has zero sibling-mod dependencies, so it can't construct e.g. Lyfe's Research
     * Bench or Settlemynts' Settlement Claim Flag directly. A caller should register from its own
     * constructor, gated behind {@code ModList.get().isLoaded("kyt")}, same soft-dependency pattern
     * used throughout this suite -- registration order across mods doesn't matter (see {@code
     * system.DevKytContributions}'s own doc for why).
     */
    public static void registerDevKytContribution(Supplier<List<ItemStack>> items) {
        SystemKyts.registerDevKytContribution(items);
    }
}
