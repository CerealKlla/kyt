package com.github.cerealklla.kyt.generic;

import java.util.Locale;
import java.util.Optional;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * The 13 equipment "families" a Kyt slot can be filled with a {@link GenericEquipmentItem}
 * placeholder for instead of one specific real item -- explicit user request, 2026-10-25: "Use any
 * Helmet"/"Use any Nature Staff"-style items, one per every Weapon/Tool/Armor family in the suite
 * (Lyfe's {@code craft.EquipmentTierLadder.ToolType} six tool/weapon types, its two fixed-tier
 * ranged weapons, its {@code craft.NatureStaffLadder}, and the four vanilla armor
 * {@link EquipmentSlot}s).
 *
 * <p>Kyt itself has zero sibling-mod dependencies by design (see this mod's own CLAUDE.md) -- this
 * enum only carries a stable identity + display shape for each family, nothing that resolves "which
 * real item is Tier 2 of this family." That resolution (walking Lyfe's own tier ladders low-to-high
 * and checking plot stock) is Settlemynts' job at Guard-equip time, the one place that already
 * depends on both Kyt and Lyfe -- see {@code bridge.KytGuardhouseBridge}/{@code
 * guardhouse.GuardEntity} on that side.
 */
public enum GenericFamily {
    SWORD("Use any Sword", null, true),
    PICKAXE("Use any Pickaxe", null, true),
    AXE("Use any Axe", null, true),
    SHOVEL("Use any Shovel", null, true),
    HOE("Use any Hoe", null, true),
    SPEAR("Use any Spear", null, true),
    BOW("Use any Bow", null, true),
    CROSSBOW("Use any Crossbow", null, true),
    NATURE_STAFF("Use any Nature Staff", null, true),
    HELMET("Use any Helmet", EquipmentSlot.HEAD, false),
    CHESTPLATE("Use any Chestplate", EquipmentSlot.CHEST, false),
    LEGGINGS("Use any Leggings", EquipmentSlot.LEGS, false),
    BOOTS("Use any Boots", EquipmentSlot.FEET, false);

    private final String displayName;
    private final EquipmentSlot armorSlot;
    private final boolean weaponLike;

    GenericFamily(String displayName, EquipmentSlot armorSlot, boolean weaponLike) {
        this.displayName = displayName;
        this.armorSlot = armorSlot;
        this.weaponLike = weaponLike;
    }

    /** The item id suffix ("generic_" + this) every {@link GenericEquipmentItem} registration uses. */
    public String itemIdSuffix() {
        return "generic_" + name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    /** Empty for the six tool/weapon and two ranged/staff families -- those stay in the unrestricted Offhand/Hotbar slots. */
    public Optional<EquipmentSlot> armorSlot() {
        return Optional.ofNullable(armorSlot);
    }

    /** Whether this family should sort under the Kyt Editor palette's "Weapons" tab instead of "Other" -- see {@code loadout.LoadoutItemFilters}. */
    public boolean weaponLike() {
        return weaponLike;
    }
}
