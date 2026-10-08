package com.github.cerealklla.kyt.loadout.client;

import com.github.cerealklla.kyt.loadout.LoadoutMenu;
import com.github.cerealklla.kyt.loadout.SaveKytPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Create/Edit screen for one Kyt loadout -- a real {@link AbstractContainerScreen} over {@link
 * LoadoutMenu}. Reuses two cropped slices of vanilla's own real {@code inventory.png} texture
 * ({@link #INVENTORY_LOCATION}, inherited from {@code AbstractContainerScreen}) -- the armor
 * column + player-model backdrop + offhand slot (x 0-96, excluding the crafting grid which starts
 * at x=98 in that same texture), and the hotbar strip -- rather than the full panel or a hand-drawn
 * one. {@link LoadoutMenu}'s slots sit at the exact coordinates vanilla's own {@code InventoryMenu}
 * uses for those same regions, so the texture's art lines up with real slots. No crafting grid, no
 * player-model black box left blank, and no "your own inventory" panel (all three removed
 * 2026-10-01 per explicit user feedback on the first visual pass).
 *
 * <p>The player-model backdrop square <b>is</b> used, per the user's own original spec ("I expect
 * to see the guard's preview model there") -- no real Guard entity exists anywhere in the suite yet
 * (guard spawning is a later, unstarted phase), so a plain {@link ArmorStand} mannequin stands in:
 * built once in {@link #init()}, never added to any real level (purely a client-side render prop),
 * re-equipped from this menu's own armor/offhand slots every frame before being handed to vanilla's
 * own {@link InventoryScreen#extractEntityInInventoryFollowsMouse} -- the exact same helper the real
 * player-inventory screen uses for its own live model preview. Calling {@code setItemSlot} every
 * frame is safe here specifically because the mannequin's level is the client level: {@code
 * LivingEntity#onEquipItem}'s equip-sound branch is guarded by {@code !level().isClientSide()}, so
 * nothing ever plays.
 *
 * <p>"Kyt Name" is disabled (read-only) in Edit mode, matching the user's own spec. "Open Palette"
 * brings up {@link ItemPaletteScreen} to browse/pick an item onto the cursor; "Save" sends {@link
 * SaveKytPayload} (no item data -- the server already has this menu's live, synced Container).
 */
public final class LoadoutEditorScreen extends AbstractContainerScreen<LoadoutMenu> {

    // Source crop of inventory.png: x 0-97 covers the armor column (x=8-26), the player-model
    // backdrop square, and the offhand slot (now x=79-97) -- the crafting grid starts at x=98
    // (InventoryMenu#addCraftingGridSlots(98, 18)), so a 97px-wide crop excludes it cleanly.
    //
    // Height dropped 90 -> 82, 2026-10-01: vanilla's own 3-row main-inventory texture starts at
    // y=84 (AbstractContainerMenu#addStandardInventorySlots's own "top=84" for that call), and a
    // 90px-tall crop dipped 6px into it -- visible in a live screenshot as a thin leftover strip of
    // that (deliberately removed, see LoadoutMenu's own doc) inventory-row art peeking out just
    // below the armor column. 82 stays clear of it with a couple px to spare below the Boots slot's
    // own bottom edge (ARMOR_TOP_Y + 54 + 18 = 80).
    private static final int TOP_CROP_WIDTH = 97;
    private static final int TOP_CROP_HEIGHT = 82;
    // Dropped 24 -> 19 (1px top border + the slot's own 18px height) same day -- the extra ~5px of
    // margin the old value carried past the hotbar row's real bottom edge reached into the panel's
    // own bottom-right corner trim (a different, narrower shape than the flat row above it), which
    // on a live screenshot showed up as the right portion of the hotbar row missing its grey slot
    // backing entirely.
    private static final int HOTBAR_CROP_HEIGHT = 19;

    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = LoadoutMenu.HOTBAR_Y + HOTBAR_CROP_HEIGHT + 30;

    // The top crop is drawn 14px lower than its own source (destY = topPos + TOP_DEST_Y_OFFSET,
    // srcY stays 0) -- added 2026-10-01 along with pulling LoadoutMenu#HOTBAR_Y up from vanilla's
    // own 142 to 110, together closing most of the large dead gap a live screenshot showed between
    // the armor panel and the hotbar row (the empty space where vanilla's own 3-row player inventory
    // would otherwise sit, were it not deliberately removed from this menu).
    private static final int TOP_DEST_Y_OFFSET = 14;
    // The hotbar background's source row hasn't moved -- it's still vanilla's own real texture
    // position (141 = HOTBAR_Y - 1 back when HOTBAR_Y was still vanilla's unmodified 142). Now that
    // LoadoutMenu#HOTBAR_Y is a destination-only coordinate, this constant keeps the source sample
    // pinned to where the grey slot art actually lives in the PNG, independent of where it's drawn.
    private static final int HOTBAR_SOURCE_V = 141;

    private static final int PREVIEW_X0 = 26;
    private static final int PREVIEW_Y0 = 8 + TOP_DEST_Y_OFFSET;
    private static final int PREVIEW_X1 = 75;
    private static final int PREVIEW_Y1 = 78 + TOP_DEST_Y_OFFSET;

    private EditBox nameBox;
    private ArmorStand previewMannequin;

    public LoadoutEditorScreen(LoadoutMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        // Flush with the panel's own left edge (was 60 -- floated the title well right of the panel
        // it's labeling, called out in a live screenshot), matching "Kyt Name:" below it.
        this.titleLabelX = 8;
        this.titleLabelY = -22;
    }

    @Override
    protected void init() {
        super.init();

        if (minecraft.level != null) {
            previewMannequin = new ArmorStand(EntityType.ARMOR_STAND, minecraft.level);
            previewMannequin.setShowArms(true);
        }

        nameBox = addRenderableWidget(new EditBox(font, leftPos + 56, topPos - 12, 112, 14, Component.literal("Kyt name")));
        nameBox.setMaxLength(48);
        nameBox.setValue(menu.name());
        nameBox.setEditable(menu.isNameEditable());

        addRenderableWidget(Button.builder(Component.literal("Open Palette"), b ->
                Minecraft.getInstance().setScreen(new ItemPaletteScreen(this)))
                .bounds(leftPos + imageWidth - 100, topPos + imageHeight - 24, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(leftPos, topPos + imageHeight - 24, 70, 20).build());
    }

    private void save() {
        String name = menu.isNameEditable() ? nameBox.getValue().trim() : menu.name();
        if (name.isEmpty()) {
            return;
        }
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(new SaveKytPayload(name)));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_LOCATION, leftPos, topPos + TOP_DEST_Y_OFFSET, 0.0F, 0.0F, TOP_CROP_WIDTH, TOP_CROP_HEIGHT, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_LOCATION, leftPos, topPos + LoadoutMenu.HOTBAR_Y - 1, 0.0F,
                (float) HOTBAR_SOURCE_V, imageWidth, HOTBAR_CROP_HEIGHT, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Deliberately doesn't call super -- vanilla's own AbstractContainerScreen#extractLabels
        // also draws playerInventoryTitle ("Inventory") at its default inventoryLabelX/Y, which made
        // no sense here (there's no player-inventory section on this screen at all, see LoadoutMenu's
        // own doc) and showed up as a faint leftover "Inventory" label in a live screenshot.
        graphics.text(font, title, titleLabelX, titleLabelY, -12566464, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(font, "Kyt Name:", leftPos, topPos - 10, 0xFFFFFFFF);

        if (previewMannequin != null) {
            previewMannequin.setItemSlot(EquipmentSlot.HEAD, menu.getSlot(LoadoutMenu.HELMET_SLOT).getItem());
            previewMannequin.setItemSlot(EquipmentSlot.CHEST, menu.getSlot(LoadoutMenu.CHESTPLATE_SLOT).getItem());
            previewMannequin.setItemSlot(EquipmentSlot.LEGS, menu.getSlot(LoadoutMenu.LEGGINGS_SLOT).getItem());
            previewMannequin.setItemSlot(EquipmentSlot.FEET, menu.getSlot(LoadoutMenu.BOOTS_SLOT).getItem());
            previewMannequin.setItemSlot(EquipmentSlot.OFFHAND, menu.getSlot(LoadoutMenu.OFFHAND_SLOT).getItem());
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, leftPos + PREVIEW_X0, topPos + PREVIEW_Y0,
                    leftPos + PREVIEW_X1, topPos + PREVIEW_Y1, 30, 0.0625F, mouseX, mouseY, previewMannequin);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // AbstractContainerScreen#keyPressed closes this screen on the "open inventory" key (E by
        // default) whenever nothing consumed the keystroke first -- and vanilla's own EditBox only
        // consumes special editing keys (backspace, arrows, copy/paste) via keyPressed, not plain
        // letters (those arrive separately via charTyped). Without this guard, typing "e" into the
        // name box falls through and closes the whole screen mid-word. Escape still closes normally.
        if (nameBox != null && nameBox.isFocused() && !event.isEscape()) {
            nameBox.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
