package com.github.cerealklla.kyt.loadout;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A real {@link AbstractContainerMenu} for creating/editing one {@link LoadoutRecord} -- Helmet,
 * Chestplate, Leggings, Boots (each restricted to its matching {@link EquipmentSlot} via {@link
 * LoadoutArmorSlot}), one unrestricted Offhand slot, and an unrestricted 9-slot Hotbar row, per the
 * user's own spec: "mirrors the real vanilla player inventory... I don't care about limiting the
 * hotbar, the other slots should be restricted to their appropriate type."
 *
 * <p>The player's own real inventory is deliberately <b>not</b> wired into this menu (removed
 * 2026-10-01 per explicit user feedback -- the crafting grid/player-model quadrant of vanilla's
 * {@code inventory.png} and an extra "your own inventory" panel both cluttered the screen for no
 * real benefit) -- the only way an item reaches a loadout slot is via {@code
 * client.ItemPaletteScreen}'s give-onto-cursor flow, then a normal slot click.
 *
 * <p>Item contents sync to the client entirely through vanilla's own container-sync machinery (no
 * hand-rolled network payload carrying {@code ItemStack}s) -- the server builds this menu from a
 * real, already-populated {@link Container} (see {@code KytEditorMenuProvider}), and the client
 * reconstructs it empty (see {@code registration.ModMenus}), filled in by the normal slot-sync
 * packets that follow. {@code name}/{@code nameEditable} are the only two fields carried over this
 * menu's own extra-data channel, via {@code IMenuProviderExtension#writeClientSideData}.
 */
public class LoadoutMenu extends AbstractContainerMenu {

    public static final int HELMET_SLOT = 0;
    public static final int CHESTPLATE_SLOT = 1;
    public static final int LEGGINGS_SLOT = 2;
    public static final int BOOTS_SLOT = 3;
    public static final int OFFHAND_SLOT = 4;
    public static final int HOTBAR_START = 5;
    public static final int HOTBAR_SIZE = 9;
    public static final int OWN_SLOT_COUNT = HOTBAR_START + HOTBAR_SIZE;

    // Exact coordinates vanilla's own InventoryMenu uses for the armor column/offhand/hotbar row,
    // so this menu can reuse cropped slices of vanilla's real `inventory.png` background texture
    // instead of a hand-drawn panel -- see client.LoadoutEditorScreen's own doc for why.
    // ARMOR_TOP_Y/OFFHAND_Y both carry a +14 offset (was 8/40) -- not vanilla's own coordinate
    // anymore, see LoadoutEditorScreen's own doc for why (the top panel's destination was pushed
    // down 14px to close a large dead gap above the hotbar row). HOTBAR_Y is independently pulled
    // up from vanilla's own 142 to 110 for the same reason (closing that gap from the other side) --
    // LoadoutEditorScreen's background blit decouples this destination coordinate from the texture's
    // own fixed source row (which hasn't moved), so this constant is purely "where the hotbar
    // row/slots sit on screen," not "where that art lives in the PNG" anymore.
    public static final int ARMOR_X = 8;
    public static final int ARMOR_TOP_Y = 22;
    // Moved up + slightly right 2026-10-01 (was 77,62 -- vanilla's own offhand coordinate, which sat
    // down by the mannequin's feet, dangling outside the preview box's own right edge with no clear
    // relationship to it per live feedback). 79 is the rightmost x that still fits inside
    // LoadoutEditorScreen's 97px-wide top texture crop (79 + 18 = 97).
    public static final int OFFHAND_X = 79;
    public static final int OFFHAND_Y = 54;
    public static final int HOTBAR_Y = 110;

    private final Container container;
    private final String name;
    private final boolean nameEditable;

    /** Server-side: a real, already-populated Container backs this menu. */
    public LoadoutMenu(MenuType<?> type, int containerId, Inventory inventory, Container container, String name, boolean nameEditable) {
        super(type, containerId);
        this.container = container;
        this.name = name;
        this.nameEditable = nameEditable;
        layoutSlots();
    }

    /** Client-side reconstruction (see {@code registration.ModMenus}) -- an empty Container, filled in by the normal sync packets that follow. */
    public LoadoutMenu(MenuType<?> type, int containerId, Inventory inventory, String name, boolean nameEditable) {
        this(type, containerId, inventory, new SimpleContainer(OWN_SLOT_COUNT), name, nameEditable);
    }

    private void layoutSlots() {
        addSlot(new LoadoutArmorSlot(container, HELMET_SLOT, ARMOR_X, ARMOR_TOP_Y, EquipmentSlot.HEAD));
        addSlot(new LoadoutArmorSlot(container, CHESTPLATE_SLOT, ARMOR_X, ARMOR_TOP_Y + 18, EquipmentSlot.CHEST));
        addSlot(new LoadoutArmorSlot(container, LEGGINGS_SLOT, ARMOR_X, ARMOR_TOP_Y + 36, EquipmentSlot.LEGS));
        addSlot(new LoadoutArmorSlot(container, BOOTS_SLOT, ARMOR_X, ARMOR_TOP_Y + 54, EquipmentSlot.FEET));
        addSlot(new OffhandSlot(container, OFFHAND_SLOT, OFFHAND_X, OFFHAND_Y));

        for (int i = 0; i < HOTBAR_SIZE; i++) {
            addSlot(new Slot(container, HOTBAR_START + i, 8 + i * 18, HOTBAR_Y));
        }
    }

    public String name() {
        return name;
    }

    public boolean isNameEditable() {
        return nameEditable;
    }

    /**
     * Snapshots this menu's own slots into a saveable {@link LoadoutRecord}. Always
     * {@code systemManaged=false} with an empty {@code inventory} overflow -- this UI never reads
     * or writes either field (see {@code LoadoutRecord}'s own doc comment); only code-constructed
     * Kyts (e.g. {@code system.SystemKyts}) use them. {@code KytMod}'s payload handlers separately
     * refuse to let this menu even open for an existing {@code systemManaged} Kyt, so reaching this
     * method with a reserved name should never happen in practice -- this is just the data shape,
     * not the enforcement point.
     */
    public LoadoutRecord toRecord(String recordName) {
        return new LoadoutRecord(
                recordName,
                container.getItem(HELMET_SLOT).copy(),
                container.getItem(CHESTPLATE_SLOT).copy(),
                container.getItem(LEGGINGS_SLOT).copy(),
                container.getItem(BOOTS_SLOT).copy(),
                container.getItem(OFFHAND_SLOT).copy(),
                hotbarCopy(),
                false,
                LoadoutRecord.emptyInventory());
    }

    private java.util.List<ItemStack> hotbarCopy() {
        java.util.List<ItemStack> hotbar = new java.util.ArrayList<>(HOTBAR_SIZE);
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            hotbar.add(container.getItem(HOTBAR_START + i).copy());
        }
        return hotbar;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        // No second slot region exists to shift-click into (the player's own inventory was
        // deliberately removed from this menu) -- shift-click is simply a no-op here.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
