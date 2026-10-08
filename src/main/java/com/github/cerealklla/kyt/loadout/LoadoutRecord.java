package com.github.cerealklla.kyt.loadout;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;

/**
 * One saved Kyt loadout -- mirrors the real vanilla player inventory shape, per the user's own
 * spec: four armor slots (Helmet/Chestplate/Leggings/Boots), one Offhand slot, and a 9-slot Hotbar.
 * No separate "Weapons"/"Consumables" slot groups -- those are browsing filters in the item palette
 * only (see {@code loadout.client.ItemPaletteScreen}), not output categories here. Unfilled slots
 * are {@link ItemStack#EMPTY}.
 *
 * <p><b>{@code inventory}/{@code systemManaged}, added 2026-10-02</b> (see decisions.md) -- a
 * 27-slot general-inventory overflow, used only by code-constructed, mod-managed Kyts (e.g.
 * {@code system.SystemKyts}' "Dev" Kyt) that need more than the 14 named slots above can hold.
 * The player-facing Kyt Editor UI (`loadout.LoadoutMenu`/`client.LoadoutEditorScreen`) never reads
 * or writes this field -- it's write-only-from-code, by design, not an oversight. {@code
 * systemManaged} marks a Kyt as mod-owned: excluded from every listing (`api.Kyt#listKytNames`)
 * and protected from creation/edit/overwrite via the normal Kyt Editor (`KytMod`'s payload
 * handlers). Both fields default via the codec for full backward compatibility with every
 * previously-saved Kyt JSON file.
 */
public record LoadoutRecord(
        String name,
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots,
        ItemStack offhand,
        List<ItemStack> hotbar,
        boolean systemManaged,
        List<ItemStack> inventory) {

    public static final int HOTBAR_SIZE = 9;
    public static final int INVENTORY_SIZE = 27;

    private static List<ItemStack> emptyList(int size) {
        List<ItemStack> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(ItemStack.EMPTY);
        }
        return list;
    }

    /** A fresh, fully-empty {@link #INVENTORY_SIZE}-slot overflow list -- for callers (e.g. {@code LoadoutMenu#toRecord}) that never populate it. */
    public static List<ItemStack> emptyInventory() {
        return emptyList(INVENTORY_SIZE);
    }

    public static LoadoutRecord empty(String name) {
        return new LoadoutRecord(name, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                emptyList(HOTBAR_SIZE), false, emptyList(INVENTORY_SIZE));
    }

    public static final Codec<LoadoutRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(LoadoutRecord::name),
            ItemStack.OPTIONAL_CODEC.fieldOf("helmet").forGetter(LoadoutRecord::helmet),
            ItemStack.OPTIONAL_CODEC.fieldOf("chestplate").forGetter(LoadoutRecord::chestplate),
            ItemStack.OPTIONAL_CODEC.fieldOf("leggings").forGetter(LoadoutRecord::leggings),
            ItemStack.OPTIONAL_CODEC.fieldOf("boots").forGetter(LoadoutRecord::boots),
            ItemStack.OPTIONAL_CODEC.fieldOf("offhand").forGetter(LoadoutRecord::offhand),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("hotbar").forGetter(LoadoutRecord::hotbar),
            Codec.BOOL.optionalFieldOf("system_managed", false).forGetter(LoadoutRecord::systemManaged),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("inventory", emptyList(INVENTORY_SIZE)).forGetter(LoadoutRecord::inventory)
    ).apply(i, LoadoutRecord::new));

    /**
     * Pads/truncates a loaded hotbar list to exactly {@link #HOTBAR_SIZE} and an inventory list to
     * exactly {@link #INVENTORY_SIZE} -- defensive against hand-edited JSON or an older save
     * decoded against the optional-field default.
     */
    public LoadoutRecord {
        if (hotbar.size() != HOTBAR_SIZE) {
            List<ItemStack> fixed = new ArrayList<>(HOTBAR_SIZE);
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                fixed.add(i < hotbar.size() ? hotbar.get(i) : ItemStack.EMPTY);
            }
            hotbar = fixed;
        }
        if (inventory.size() != INVENTORY_SIZE) {
            List<ItemStack> fixed = new ArrayList<>(INVENTORY_SIZE);
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                fixed.add(i < inventory.size() ? inventory.get(i) : ItemStack.EMPTY);
            }
            inventory = fixed;
        }
    }
}
