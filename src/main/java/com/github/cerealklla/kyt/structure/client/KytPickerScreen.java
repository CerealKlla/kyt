package com.github.cerealklla.kyt.structure.client;

import com.github.cerealklla.kyt.loadout.RequestEditKytPayload;
import com.github.cerealklla.kyt.structure.OpenKytPickerPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * "Edit Kyt" name picker -- a paged Prev/Next cycle through saved names, same style as Blueprynts'
 * {@code construction.BlueprintPickerScreen} (no scrollable list widget exists anywhere in this
 * suite yet).
 */
public final class KytPickerScreen extends Screen {

    private final OpenKytPickerPayload data;
    private int index;
    private Button editButton;

    public KytPickerScreen(OpenKytPickerPayload data) {
        super(Component.literal("Edit Kyt"));
        this.data = data;
    }

    @Override
    protected void init() {
        boolean hasEntries = !data.names().isEmpty();
        int centerX = width / 2;
        int y = 60;

        addRenderableWidget(Button.builder(Component.literal("<"), b -> page(-1))
                .bounds(centerX - 110, y, 20, 20).build()).active = hasEntries;
        addRenderableWidget(Button.builder(Component.literal(">"), b -> page(1))
                .bounds(centerX + 90, y, 20, 20).build()).active = hasEntries;

        editButton = addRenderableWidget(Button.builder(Component.literal("Edit This Kyt"), b -> editCurrent())
                .bounds(centerX - 75, y + 30, 150, 20).build());
        editButton.active = hasEntries;

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(centerX - 50, y + 60, 100, 20).build());
    }

    private void page(int delta) {
        int count = data.names().size();
        if (count == 0) {
            return;
        }
        index = Math.floorMod(index + delta, count);
    }

    private void editCurrent() {
        if (data.names().isEmpty()) {
            return;
        }
        send(new RequestEditKytPayload(data.entityId(), data.names().get(index)));
    }

    private void send(CustomPacketPayload payload) {
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(payload));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int titleWidth = font.width(title);
        graphics.text(font, title, width / 2 - titleWidth / 2, 30, 0xFFFFFFFF);

        int centerX = width / 2;
        int y = 60;
        if (data.names().isEmpty()) {
            String empty = "No saved Kyts yet.";
            graphics.text(font, empty, centerX - font.width(empty) / 2, y + 4, 0xFFAAAAAA);
            return;
        }
        String name = data.names().get(index);
        String position = "(" + (index + 1) + " / " + data.names().size() + ")";
        graphics.text(font, name, centerX - font.width(name) / 2, y + 4, 0xFFFFFFFF);
        graphics.text(font, position, centerX - font.width(position) / 2, y + 16, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
