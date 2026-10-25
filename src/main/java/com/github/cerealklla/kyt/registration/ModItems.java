package com.github.cerealklla.kyt.registration;

import java.util.EnumMap;
import java.util.Map;

import com.github.cerealklla.kyt.KytMod;
import com.github.cerealklla.kyt.generic.GenericEquipmentItem;
import com.github.cerealklla.kyt.generic.GenericFamily;
import com.github.cerealklla.kyt.structure.KytConfigurationStandItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    private ModItems() {
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KytMod.MODID);

    public static final DeferredItem<KytConfigurationStandItem> KYT_CONFIGURATION_STAND = ITEMS.register(
            "kyt_configuration_stand",
            id -> new KytConfigurationStandItem(new Item.Properties()
                    .stacksTo(64)
                    .setId(ResourceKey.create(Registries.ITEM, id))));

    /**
     * One {@link GenericEquipmentItem} per {@link GenericFamily} (explicit user request, 2026-10-25:
     * "add new items for Kyt: Generics... for every single Weapon, Tool, Armor"). The four armor
     * families carry a plain {@code DataComponents.EQUIPPABLE} (no attribute bonuses -- these are
     * never real armor) purely so {@code loadout.LoadoutArmorSlot#mayPlace} accepts them in the Kyt
     * Editor's matching restricted slot column; the nine tool/weapon/ranged/staff families need no
     * such component since Offhand/Hotbar slots already accept anything.
     */
    public static final Map<GenericFamily, DeferredItem<GenericEquipmentItem>> GENERIC_EQUIPMENT = registerGenerics();

    private static Map<GenericFamily, DeferredItem<GenericEquipmentItem>> registerGenerics() {
        Map<GenericFamily, DeferredItem<GenericEquipmentItem>> result = new EnumMap<>(GenericFamily.class);
        for (GenericFamily family : GenericFamily.values()) {
            result.put(family, ITEMS.register(family.itemIdSuffix(), id -> new GenericEquipmentItem(family,
                    family.armorSlot()
                            .map(slot -> new Item.Properties()
                                    .stacksTo(64)
                                    .component(DataComponents.EQUIPPABLE, Equippable.builder(slot).build()))
                            .orElseGet(() -> new Item.Properties().stacksTo(64))
                            .setId(ResourceKey.create(Registries.ITEM, id)))));
        }
        return result;
    }
}
