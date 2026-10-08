package com.github.cerealklla.kyt.loadout;

import com.github.cerealklla.kyt.registration.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuProviderExtension;

/**
 * Opens {@link LoadoutMenu} via {@code ServerPlayer#openMenu} -- {@code name}/{@code nameEditable}
 * ride along on this menu's own extra-data channel ({@link #writeClientSideData}), read back by
 * {@code registration.ModMenus}' client-reconstruction factory.
 */
public final class KytEditorMenuProvider implements MenuProvider, IMenuProviderExtension {

    private final String name;
    private final boolean nameEditable;
    private final LoadoutRecord current;

    public KytEditorMenuProvider(String name, boolean nameEditable, LoadoutRecord current) {
        this.name = name;
        this.nameEditable = nameEditable;
        this.current = current;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Kyt Editor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        Container container = new SimpleContainer(LoadoutMenu.OWN_SLOT_COUNT);
        container.setItem(LoadoutMenu.HELMET_SLOT, current.helmet().copy());
        container.setItem(LoadoutMenu.CHESTPLATE_SLOT, current.chestplate().copy());
        container.setItem(LoadoutMenu.LEGGINGS_SLOT, current.leggings().copy());
        container.setItem(LoadoutMenu.BOOTS_SLOT, current.boots().copy());
        container.setItem(LoadoutMenu.OFFHAND_SLOT, current.offhand().copy());
        for (int i = 0; i < LoadoutRecord.HOTBAR_SIZE; i++) {
            ItemStack stack = current.hotbar().get(i);
            container.setItem(LoadoutMenu.HOTBAR_START + i, stack.copy());
        }
        return new LoadoutMenu(ModMenus.LOADOUT.get(), containerId, inventory, container, name, nameEditable);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeBoolean(nameEditable);
    }
}
