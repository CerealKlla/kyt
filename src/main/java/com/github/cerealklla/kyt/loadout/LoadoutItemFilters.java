package com.github.cerealklla.kyt.loadout;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

/**
 * Shared item-category predicates, used both to restrict {@link LoadoutArmorSlot}'s own {@code
 * mayPlace} and to classify items into {@code client.ItemPaletteScreen}'s three browsing tabs --
 * kept in one place so the two can never drift out of sync with each other.
 */
public final class LoadoutItemFilters {

    private LoadoutItemFilters() {
    }

    public static boolean isArmor(ItemStack stack) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot().isArmor();
    }

    /**
     * No clean vanilla "is a weapon" tag exists -- this is a heuristic (anything carrying a combat
     * data component, plus the vanilla swords tag), not an authoritative classification. Flagged as
     * a known imprecision, not an oversight -- it only affects which palette tab an item is easiest
     * to find under, never which loadout slots accept it (Offhand/Hotbar take anything).
     */
    public static boolean isWeapon(ItemStack stack) {
        return stack.get(DataComponents.WEAPON) != null
                || stack.get(DataComponents.PIERCING_WEAPON) != null
                || stack.get(DataComponents.KINETIC_WEAPON) != null
                || stack.is(ItemTags.SWORDS);
    }

    public static boolean isConsumable(ItemStack stack) {
        return stack.get(DataComponents.FOOD) != null || stack.get(DataComponents.CONSUMABLE) != null;
    }
}
