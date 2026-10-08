package com.github.cerealklla.kyt.registration;

import com.github.cerealklla.kyt.KytMod;
import com.github.cerealklla.kyt.structure.KytConfigurationStandItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
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
}
