package com.github.cerealklla.kyt.loadout;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

/** The unrestricted Offhand slot of a {@link LoadoutMenu} -- plain, except it shows vanilla's real empty-offhand (shield) ghost icon when unfilled, matching {@code InventoryMenu}'s own offhand slot look. */
public class OffhandSlot extends Slot {

    public OffhandSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public Identifier getNoItemIcon() {
        return InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
    }
}
