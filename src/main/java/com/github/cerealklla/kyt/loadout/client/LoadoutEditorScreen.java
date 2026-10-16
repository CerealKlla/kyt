package com.github.cerealklla.kyt.loadout.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.cerealklla.kyt.loadout.LoadoutItemFilters;
import com.github.cerealklla.kyt.loadout.LoadoutMenu;
import com.github.cerealklla.kyt.loadout.RequestPaletteItemPayload;
import com.github.cerealklla.kyt.loadout.SaveKytPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
 * <p>"Kyt Name" is disabled (read-only) in Edit mode, matching the user's own spec.
 *
 * <p><b>Item palette merged directly into this screen, 2026-10-13</b> -- real live-test feedback:
 * "very clunky trying to take items from the palette to add" when it was its own separate {@code
 * Screen} (the old {@code ItemPaletteScreen}), reached via an "Open Palette" button and left via a
 * "Back" button -- picking an item took four real clicks (Open Palette -> pick item -> Back -> click
 * destination slot) for what should be two. The palette's own tabs/grid/catalog logic is unchanged
 * (still a right-click-for-one/left-click-for-a-stack creative-style browser, same {@link
 * RequestPaletteItemPayload} round trip -- picking an item never touches this screen's state
 * directly, the server sets it on the live {@link LoadoutMenu} both panels already share), just
 * folded in as a permanently-visible panel to the right of the loadout slots instead of a whole
 * separate screen -- so picking an item and placing it is now one continuous click-click action on
 * the one screen, no navigation in between.
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

    // The loadout panel's own real width -- kept as its own constant (distinct from this screen's
    // now-wider combined `imageWidth`, see this class's own doc) specifically so the hotbar-row blit
    // below keeps sampling vanilla's real texture at its own true width instead of being stretched
    // across the palette panel too.
    private static final int LOADOUT_PANEL_WIDTH = 176;
    private static final int PALETTE_GAP = 10;

    private static final int IMAGE_WIDTH = LOADOUT_PANEL_WIDTH + PALETTE_GAP + ItemPalette.PANEL_WIDTH;
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
    private final ItemPalette palette = new ItemPalette();
    private int paletteLeft;
    private int paletteTop;

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

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(leftPos, topPos + imageHeight - 24, 70, 20).build());

        paletteLeft = leftPos + LOADOUT_PANEL_WIDTH + PALETTE_GAP;
        paletteTop = topPos + TOP_DEST_Y_OFFSET;
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
                (float) HOTBAR_SOURCE_V, LOADOUT_PANEL_WIDTH, HOTBAR_CROP_HEIGHT, 256, 256);
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

        palette.extractRenderState(graphics, font, paletteLeft, paletteTop, mouseX, mouseY);
    }

    /**
     * Real live report, 2026-10-16: "isn't actually allowing you to click anything in the pallet."
     * Root cause, confirmed against the decompiled vanilla source: {@code imageWidth}/{@code
     * imageHeight} were widened to cover the palette panel too (see this class's own doc, the
     * 2026-10-13 merge), but vanilla's own {@code AbstractContainerScreen#mouseClicked} always
     * returns {@code true} unconditionally once it reaches its own final line -- even for a click
     * that matched no real {@code Slot} -- as long as {@code hasClickedOutside} says the click
     * landed within those same {@code imageWidth}/{@code imageHeight} bounds. Since the palette now
     * sits inside those bounds, every click there was being silently swallowed by {@code
     * super.mouseClicked} before {@code palette.mouseClicked} was ever reached. Fixed by checking the
     * palette's own real bounds (grid + tab row) FIRST and routing straight to it, bypassing {@code
     * super.mouseClicked} entirely for that region instead of only falling back to the palette on a
     * {@code false} that vanilla was never actually going to return there.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (palette.isOverPalette(event.x(), event.y(), paletteLeft, paletteTop)) {
            return palette.mouseClicked(event, paletteLeft, paletteTop);
        }
        return super.mouseClicked(event, doubled);
    }

    /**
     * General fix, 2026-10-16, replacing two narrower region-specific patches (see decisions.md) --
     * a third live report ("Clicking with an item in hand anywhere in this region of the screen" --
     * a screenshot of the real vanilla HUD strip visible below/around this screen's own panel,
     * nowhere near the Name box or palette tabs the earlier two patches targeted) made it clear the
     * underlying bug is general, not confined to specific named regions. Confirmed against the
     * decompiled vanilla source ({@code AbstractContainerScreen#mouseClicked}/{@code mouseReleased}
     * both compute {@code clickedOutside} via this exact method, and "outside" means outside {@code
     * imageWidth}/{@code imageHeight} -- this screen's own panel is far smaller than the full window,
     * so the real vanilla HUD (hotbar/health/armor) stays visible in the uncovered area below it,
     * and clicking/releasing anywhere out there was dropping a carried item for free.
     *
     * <p>Forcing this to always return {@code false} is safe, not just convenient: confirmed against
     * {@code AbstractContainerMenu#doClick}'s own real source that every non-{@code -999} branch of
     * its click handling starts with {@code if (slotIndex < 0) return;} -- so suppressing the -999
     * "drop/throw" sentinel entirely still leaves a completely safe no-op for any click/release that
     * isn't over a real {@link net.minecraft.world.inventory.Slot}, exactly matching ordinary vanilla
     * behavior for clicking dead background space *inside* a normal container screen's own panel.
     * This single override supersedes (and is why the two region-specific {@code mouseReleased}
     * bypasses that used to sit here were removed) every variant of this bug in one place, since it's
     * no longer possible for ANY out-of-panel click to be treated as "outside" at all, regardless of
     * which specific area of the screen it happens to be over.
     */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int xo, int yo) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (palette.isOverGrid(mouseX, mouseY, paletteLeft, paletteTop)) {
            palette.scroll(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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

    /**
     * The creative-tab-style item browser, folded directly into {@link LoadoutEditorScreen} (see
     * that class's own doc for why) -- a plain helper object rather than its own {@code Screen}, so
     * it can render/handle input inline alongside the loadout panel on one shared screen. Reuses
     * vanilla's own real background texture ({@code textures/gui/container/creative_inventory/
     * tab_items.png}, which already has the 9x5 slot grid baked into its art at the same 18px-cell,
     * (9,18)-origin layout {@code ItemPickerMenu} itself uses) and the real tab sprites, so this
     * looks like a real creative screen instead of a hand-drawn grid.
     *
     * <p>Armor / Weapons / Consumables are pure browsing filters, used only to narrow which items
     * are shown -- the actual destination slot (Offhand/Hotbar accept anything, the four armor
     * slots check their own type) is enforced by {@link LoadoutMenu}'s own {@code Slot}s.
     *
     * <p>Left-click gives a full stack onto the cursor, right-click gives one. Picking an item never
     * touches this screen's own state directly -- it round-trips through the server via {@link
     * RequestPaletteItemPayload}, which sets it on the live {@link LoadoutMenu} this panel's own
     * parent screen already shares.
     */
    private static final class ItemPalette {

        private enum Category { ARMOR, WEAPONS, CONSUMABLES }

        private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/creative_inventory/tab_items.png");
        private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
        private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");
        private static final Identifier[] UNSELECTED_TABS = {
                Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_1"),
                Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_2"),
                Identifier.withDefaultNamespace("container/creative_inventory/tab_top_unselected_3"),
        };
        private static final Identifier[] SELECTED_TABS = {
                Identifier.withDefaultNamespace("container/creative_inventory/tab_top_selected_1"),
                Identifier.withDefaultNamespace("container/creative_inventory/tab_top_selected_2"),
                Identifier.withDefaultNamespace("container/creative_inventory/tab_top_selected_3"),
        };

        // Matches vanilla's own ItemPickerMenu slot layout exactly: 9 columns, 5 visible rows, 18px
        // cells, grid origin (9, 18) relative to the panel -- so the grid lines up with the texture's
        // own baked-in slot art instead of needing a separate per-cell sprite.
        private static final int COLUMNS = 9;
        private static final int VISIBLE_ROWS = 5;
        private static final int CELL_SIZE = 18;
        private static final int GRID_X = 9;
        private static final int GRID_Y = 18;
        static final int PANEL_WIDTH = 195;
        private static final int PANEL_HEIGHT = 136;
        private static final int TAB_WIDTH = 26;
        private static final int TAB_HEIGHT = 32;

        private final List<ItemStack> armorItems = new ArrayList<>();
        private final List<ItemStack> weaponItems = new ArrayList<>();
        private final List<ItemStack> consumableItems = new ArrayList<>();

        private Category active = Category.ARMOR;
        private int scrollRow;

        ItemPalette() {
            List<ItemStack> all = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) {
                    continue;
                }
                all.add(new ItemStack(item));
            }
            all.sort(Comparator.comparing(stack -> stack.getHoverName().getString()));
            for (ItemStack stack : all) {
                if (LoadoutItemFilters.isArmor(stack)) {
                    armorItems.add(stack);
                }
                if (LoadoutItemFilters.isWeapon(stack)) {
                    weaponItems.add(stack);
                }
                if (LoadoutItemFilters.isConsumable(stack)) {
                    consumableItems.add(stack);
                }
            }
        }

        private List<ItemStack> activeItems() {
            return switch (active) {
                case ARMOR -> armorItems;
                case WEAPONS -> weaponItems;
                case CONSUMABLES -> consumableItems;
            };
        }

        private static Component tabLabel(Category category) {
            return switch (category) {
                case ARMOR -> Component.literal("Armor");
                case WEAPONS -> Component.literal("Weapons");
                case CONSUMABLES -> Component.literal("Consumables");
            };
        }

        private static ItemStack tabIcon(Category category) {
            return switch (category) {
                case ARMOR -> new ItemStack(Items.DIAMOND_CHESTPLATE);
                case WEAPONS -> new ItemStack(Items.DIAMOND_SWORD);
                case CONSUMABLES -> new ItemStack(Items.APPLE);
            };
        }

        private int maxScrollRow() {
            int rows = (activeItems().size() + COLUMNS - 1) / COLUMNS;
            return Math.max(0, rows - VISIBLE_ROWS);
        }

        private static int tabX(int panelLeft, int index) {
            return panelLeft + index * (TAB_WIDTH + 2);
        }

        private static int tabY(int panelTop) {
            return panelTop - TAB_HEIGHT + 4;
        }

        boolean isOverGrid(double mouseX, double mouseY, int panelLeft, int panelTop) {
            return mouseX >= panelLeft && mouseX < panelLeft + PANEL_WIDTH
                    && mouseY >= panelTop && mouseY < panelTop + PANEL_HEIGHT;
        }

        /** Union of the grid panel and the tab row above it (which sits outside {@link #isOverGrid}'s
         *  own bounds, above {@code panelTop}) -- the real click-routing boundary {@code
         *  LoadoutEditorScreen#mouseClicked} needs, covering everything {@link #mouseClicked} itself
         *  can actually act on. */
        boolean isOverPalette(double mouseX, double mouseY, int panelLeft, int panelTop) {
            if (isOverGrid(mouseX, mouseY, panelLeft, panelTop)) {
                return true;
            }
            int tabTop = tabY(panelTop);
            return mouseX >= panelLeft && mouseX < panelLeft + Category.values().length * (TAB_WIDTH + 2)
                    && mouseY >= tabTop && mouseY < tabTop + TAB_HEIGHT;
        }

        void scroll(double scrollY) {
            scrollRow = Math.max(0, Math.min(maxScrollRow(), scrollRow - (int) Math.signum(scrollY)));
        }

        boolean mouseClicked(MouseButtonEvent event, int panelLeft, int panelTop) {
            Category[] categories = Category.values();
            for (int i = 0; i < categories.length; i++) {
                int x = tabX(panelLeft, i);
                int y = tabY(panelTop);
                if (event.x() >= x && event.x() < x + TAB_WIDTH && event.y() >= y && event.y() < y + TAB_HEIGHT) {
                    active = categories[i];
                    scrollRow = 0;
                    return true;
                }
            }

            int gridX = panelLeft + GRID_X;
            int gridY = panelTop + GRID_Y;
            int col = (int) ((event.x() - gridX) / CELL_SIZE);
            int row = (int) ((event.y() - gridY) / CELL_SIZE);
            if (col < 0 || col >= COLUMNS || row < 0 || row >= VISIBLE_ROWS) {
                return false;
            }
            int index = (scrollRow + row) * COLUMNS + col;
            List<ItemStack> items = activeItems();
            if (index < 0 || index >= items.size()) {
                return false;
            }
            ItemStack template = items.get(index);
            int count = event.button() == 1 ? 1 : template.getMaxStackSize();
            Identifier itemId = BuiltInRegistries.ITEM.getKey(template.getItem());
            Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(new RequestPaletteItemPayload(itemId, count)));
            return true;
        }

        private void extractTab(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font, int panelLeft, int panelTop, int mouseX, int mouseY, Category category, int index) {
            boolean selected = category == active;
            Identifier sprite = (selected ? SELECTED_TABS : UNSELECTED_TABS)[index];
            int x = tabX(panelLeft, index);
            int y = tabY(panelTop);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, TAB_WIDTH, TAB_HEIGHT);
            graphics.item(tabIcon(category), x + 5, y + 8);
            if (mouseX >= x + 3 && mouseX < x + TAB_WIDTH - 3 && mouseY >= y + 3 && mouseY < y + TAB_HEIGHT - 3) {
                graphics.setTooltipForNextFrame(font, tabLabel(category), mouseX, mouseY);
            }
        }

        void extractRenderState(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font, int panelLeft, int panelTop, int mouseX, int mouseY) {
            // Unselected tabs first, then the panel, then the selected tab on top -- same order
            // vanilla's own CreativeModeInventoryScreen uses, so the selected tab's bottom edge
            // paints over the panel seam instead of the panel painting over it.
            Category[] categories = Category.values();
            for (int i = 0; i < categories.length; i++) {
                if (categories[i] != active) {
                    extractTab(graphics, font, panelLeft, panelTop, mouseX, mouseY, categories[i], i);
                }
            }

            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, panelLeft, panelTop, 0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

            for (int i = 0; i < categories.length; i++) {
                if (categories[i] == active) {
                    extractTab(graphics, font, panelLeft, panelTop, mouseX, mouseY, categories[i], i);
                }
            }

            if (maxScrollRow() > 0) {
                Identifier scrollSprite = scrollRow < maxScrollRow() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
                int trackHeight = VISIBLE_ROWS * CELL_SIZE - 17;
                int scrollY = panelTop + GRID_Y + (maxScrollRow() == 0 ? 0 : trackHeight * scrollRow / maxScrollRow());
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, scrollSprite, panelLeft + PANEL_WIDTH - 14, scrollY, 12, 15);
            }

            List<ItemStack> items = activeItems();
            int gridX = panelLeft + GRID_X;
            int gridY = panelTop + GRID_Y;
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    int index = (scrollRow + row) * COLUMNS + col;
                    if (index >= items.size()) {
                        continue;
                    }
                    int x = gridX + col * CELL_SIZE;
                    int y = gridY + row * CELL_SIZE;
                    ItemStack stack = items.get(index);
                    graphics.item(stack, x + 1, y + 1);
                    if (mouseX >= x && mouseX < x + CELL_SIZE && mouseY >= y && mouseY < y + CELL_SIZE) {
                        graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
                    }
                }
            }
        }
    }
}
