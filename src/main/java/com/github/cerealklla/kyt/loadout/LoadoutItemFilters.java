package com.github.cerealklla.kyt.loadout;

import com.github.cerealklla.kyt.generic.GenericEquipmentItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

/**
 * Shared item-category predicates, used both to restrict {@link LoadoutArmorSlot}'s own {@code
 * mayPlace} and to classify items into {@code client.LoadoutEditorScreen}'s palette panel's three
 * browsing tabs -- kept in one place so the two can never drift out of sync with each other.
 */
public final class LoadoutItemFilters {

    private LoadoutItemFilters() {
    }

    public static boolean isArmor(ItemStack stack) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot().isArmor();
    }

    /**
     * No clean vanilla "is a weapon" tag exists -- this is a heuristic, not an authoritative
     * classification. Flagged as a known imprecision, not an oversight -- it only affects which
     * palette tab an item is easiest to find under, never which loadout slots accept it
     * (Offhand/Hotbar take anything). Confirmed against this MC version's real decompiled source
     * (2026-10-16): {@code DataComponents.WEAPON} is attached to every tool tier (sword/axe/pickaxe/
     * shovel/hoe alike, via {@code ToolMaterial.applyToolProperties}), not just combat items, so it's
     * both necessary (it's the only signal Mace/Trident carry) and already over-broad on its own.
     * Bow/Crossbow carry none of the three weapon components and aren't tagged `swords` -- vanilla's
     * only shared type for them is {@link ProjectileWeaponItem}, their one common supertype.
     */
    public static boolean isWeapon(ItemStack stack) {
        return stack.get(DataComponents.WEAPON) != null
                || stack.get(DataComponents.PIERCING_WEAPON) != null
                || stack.get(DataComponents.KINETIC_WEAPON) != null
                || stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SPEARS)
                || stack.getItem() instanceof ProjectileWeaponItem
                // Generic placeholders (generic.GenericEquipmentItem) carry none of the above --
                // their own family flags whether they belong here instead.
                || GenericEquipmentItem.familyOf(stack).map(f -> f.weaponLike()).orElse(false);
    }

    public static boolean isConsumable(ItemStack stack) {
        return stack.get(DataComponents.FOOD) != null || stack.get(DataComponents.CONSUMABLE) != null;
    }
}
