package com.github.cerealklla.kyt.loadout;

import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * One of the four armor slots (Helmet/Chestplate/Leggings/Boots) of a {@link LoadoutMenu} --
 * restricted to the matching {@link EquipmentSlot}. Vanilla's own {@code Slot#mayPlace} defaults to
 * unconditionally {@code true} and never consults {@code Container#canPlaceItem} on its own (a real
 * bug class already hit once in this suite -- see Settlemynts' {@code guardhouse.GuardhouseFoodSlot}
 * doc), so this dedicated subclass is the actual enforcement point, not a plain {@code Container}
 * override.
 *
 * <p>{@link #getNoItemIcon()} reuses vanilla's own real empty-armor-slot ghost sprites (the same
 * ones {@code InventoryMenu}'s own armor column uses) so an empty slot looks exactly like vanilla's,
 * not a blank square.
 */
public class LoadoutArmorSlot extends Slot {

    private static final Map<EquipmentSlot, Identifier> EMPTY_ICONS = Map.of(
            EquipmentSlot.HEAD, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            EquipmentSlot.CHEST, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            EquipmentSlot.LEGS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            EquipmentSlot.FEET, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS);

    private final EquipmentSlot equipmentSlot;

    public LoadoutArmorSlot(Container container, int index, int x, int y, EquipmentSlot equipmentSlot) {
        super(container, index, x, y);
        this.equipmentSlot = equipmentSlot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == equipmentSlot;
    }

    @Override
    public Identifier getNoItemIcon() {
        return EMPTY_ICONS.get(equipmentSlot);
    }
}
