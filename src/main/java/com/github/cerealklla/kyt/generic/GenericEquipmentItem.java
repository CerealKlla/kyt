package com.github.cerealklla.kyt.generic;

import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A placeholder item for one {@link GenericFamily} -- see that enum's own doc for the full picture.
 * Carries no real stats/durability/combat behavior of its own; it only ever exists as data sitting
 * in a saved Kyt's armor/hotbar slot, resolved into a real item of the matching family (lowest tier
 * currently in plot stock) by Settlemynts at Guard-equip time. Never meant to actually be worn/used
 * in survival -- a Guard never holds one of these itself, only ever the real item it resolves to.
 */
public class GenericEquipmentItem extends Item {

    private final GenericFamily family;

    public GenericEquipmentItem(GenericFamily family, Properties properties) {
        super(properties);
        this.family = family;
    }

    public GenericFamily family() {
        return family;
    }

    public static Optional<GenericFamily> familyOf(ItemStack stack) {
        return stack.getItem() instanceof GenericEquipmentItem generic ? Optional.of(generic.family) : Optional.empty();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.literal("A Kyt placeholder -- a Guard equipped with this resolves it to the lowest tier of this family currently in the plot's stock."));
    }
}
