package com.github.cerealklla.kyt.loadout.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.cerealklla.kyt.loadout.LoadoutItemFilters;
import com.github.cerealklla.kyt.loadout.RequestPaletteItemPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The creative-tab-style item browser -- no in-suite precedent for this (confirmed: the only
 * existing "pick one of several saved things" screens in this suite are paged Prev/Next cycles, not
 * a grid), built directly against vanilla's own {@code CreativeModeInventoryScreen} conventions
 * rather than any in-suite base class. Reuses vanilla's real background texture
 * ({@code textures/gui/container/creative_inventory/tab_items.png}, which already has the 9x5 slot
 * grid baked into its art at the same 18px-cell, (9,18)-origin layout {@code ItemPickerMenu} itself
 * uses) and the real tab sprites ({@code container/creative_inventory/tab_top_selected_*}/
 * {@code _unselected_*}) for the three category tabs, so this looks like a real creative screen
 * instead of a hand-drawn grid.
 *
 * <p>Armor / Weapons / Consumables are pure <b>browsing filters</b> here, used only to narrow which
 * items are shown -- the actual destination slot (Offhand/Hotbar accept anything, the four armor
 * slots check their own type) is enforced by {@code LoadoutMenu}'s own {@code Slot}s, not by this
 * screen.
 *
 * <p>Left-click gives a full stack onto the cursor, right-click gives one -- same vanilla
 * creative-give semantics the user's own spec referenced. Picking an item never touches this
 * screen's own state directly (see {@link RequestPaletteItemPayload}'s own doc for why) -- it
 * round-trips through the server, which sets it on the live {@code LoadoutMenu} both screens share.
 */
public final class ItemPaletteScreen extends Screen {

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
    private static final int IMAGE_WIDTH = 195;
    private static final int IMAGE_HEIGHT = 136;
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;

    private final LoadoutEditorScreen parent;
    private final List<ItemStack> armorItems = new ArrayList<>();
    private final List<ItemStack> weaponItems = new ArrayList<>();
    private final List<ItemStack> consumableItems = new ArrayList<>();

    private Category active = Category.ARMOR;
    private int scrollRow;
    private int leftPos;
    private int topPos;

    public ItemPaletteScreen(LoadoutEditorScreen parent) {
        super(Component.literal("Item Palette"));
        this.parent = parent;
        buildCatalog();
    }

    private void buildCatalog() {
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

    private Component tabLabel(Category category) {
        return switch (category) {
            case ARMOR -> Component.literal("Armor");
            case WEAPONS -> Component.literal("Weapons");
            case CONSUMABLES -> Component.literal("Consumables");
        };
    }

    private ItemStack tabIcon(Category category) {
        return switch (category) {
            case ARMOR -> new ItemStack(Items.DIAMOND_CHESTPLATE);
            case WEAPONS -> new ItemStack(Items.DIAMOND_SWORD);
            case CONSUMABLES -> new ItemStack(Items.APPLE);
        };
    }

    @Override
    protected void init() {
        leftPos = (width - IMAGE_WIDTH) / 2;
        topPos = (height - IMAGE_HEIGHT) / 2;

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> Minecraft.getInstance().setScreen(parent))
                .bounds(leftPos, topPos + IMAGE_HEIGHT + 6, IMAGE_WIDTH, 20).build());
    }

    private void switchTab(Category category) {
        active = category;
        scrollRow = 0;
    }

    private int maxScrollRow() {
        int rows = (activeItems().size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - VISIBLE_ROWS);
    }

    private int tabX(int index) {
        return leftPos + index * (TAB_WIDTH + 2);
    }

    private int tabY() {
        return topPos - TAB_HEIGHT + 4;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        scrollRow = Math.max(0, Math.min(maxScrollRow(), scrollRow - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            int x = tabX(i);
            int y = tabY();
            if (event.x() >= x && event.x() < x + TAB_WIDTH && event.y() >= y && event.y() < y + TAB_HEIGHT) {
                switchTab(categories[i]);
                return true;
            }
        }

        int gridX = leftPos + GRID_X;
        int gridY = topPos + GRID_Y;
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

    private void extractTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Category category, int index) {
        boolean selected = category == active;
        Identifier sprite = (selected ? SELECTED_TABS : UNSELECTED_TABS)[index];
        int x = tabX(index);
        int y = tabY();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, TAB_WIDTH, TAB_HEIGHT);
        graphics.item(tabIcon(category), x + 5, y + 8);
        if (mouseX >= x + 3 && mouseX < x + TAB_WIDTH - 3 && mouseY >= y + 3 && mouseY < y + TAB_HEIGHT - 3) {
            graphics.setTooltipForNextFrame(font, tabLabel(category), mouseX, mouseY);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Unselected tabs first, then the panel, then the selected tab on top -- same order vanilla's
        // own CreativeModeInventoryScreen uses, so the selected tab's bottom edge paints over the
        // panel seam instead of the panel painting over it.
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            if (categories[i] != active) {
                extractTab(graphics, mouseX, mouseY, categories[i], i);
            }
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, IMAGE_WIDTH, IMAGE_HEIGHT, 256, 256);

        for (int i = 0; i < categories.length; i++) {
            if (categories[i] == active) {
                extractTab(graphics, mouseX, mouseY, categories[i], i);
            }
        }

        if (maxScrollRow() > 0) {
            Identifier scrollSprite = scrollRow < maxScrollRow() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
            int trackHeight = VISIBLE_ROWS * CELL_SIZE - 17;
            int scrollY = topPos + GRID_Y + (maxScrollRow() == 0 ? 0 : trackHeight * scrollRow / maxScrollRow());
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, scrollSprite, leftPos + IMAGE_WIDTH - 14, scrollY, 12, 15);
        }

        List<ItemStack> items = activeItems();
        int gridX = leftPos + GRID_X;
        int gridY = topPos + GRID_Y;
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
