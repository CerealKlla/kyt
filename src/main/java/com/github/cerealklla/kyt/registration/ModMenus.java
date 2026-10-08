package com.github.cerealklla.kyt.registration;

import com.github.cerealklla.kyt.KytMod;
import com.github.cerealklla.kyt.loadout.LoadoutMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    private ModMenus() {
    }

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, KytMod.MODID);

    // Client-side reconstruction reads back the name/nameEditable extra data written by
    // KytEditorMenuProvider#writeClientSideData -- the actual item contents arrive separately via
    // vanilla's normal slot-sync packets.
    public static final DeferredHolder<MenuType<?>, MenuType<LoadoutMenu>> LOADOUT = MENU_TYPES.register(
            "loadout",
            () -> IMenuTypeExtension.create((windowId, inventory, buf) ->
                    new LoadoutMenu(null, windowId, inventory, buf.readUtf(), buf.readBoolean())));
}
