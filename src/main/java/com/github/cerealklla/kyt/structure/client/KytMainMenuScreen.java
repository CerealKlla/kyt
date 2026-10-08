package com.github.cerealklla.kyt.structure.client;

import com.github.cerealklla.kyt.loadout.RequestCreateKytPayload;
import com.github.cerealklla.kyt.structure.OpenKytMainMenuPayload;
import com.github.cerealklla.kyt.structure.RequestKytNameListPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * "Kyt Configuration" structure's Main Menu -- opened via {@link OpenKytMainMenuPayload}. "Create
 * new Kyt" expands an inline name box + Confirm in place of the two main buttons (no separate
 * screen needed for a single text field); "Edit Kyt" requests the saved-name list and opens {@link
 * KytPickerScreen}.
 */
public final class KytMainMenuScreen extends Screen {

    private final int entityId;
    private boolean creating;
    private EditBox nameBox;

    public KytMainMenuScreen(OpenKytMainMenuPayload data) {
        super(Component.literal("Kyt Configuration"));
        this.entityId = data.entityId();
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int y = 60;

        if (!creating) {
            addRenderableWidget(Button.builder(Component.literal("Create new Kyt"), b -> {
                creating = true;
                rebuildWidgets();
            }).bounds(centerX - 75, y, 150, 20).build());

            addRenderableWidget(Button.builder(Component.literal("Edit Kyt"), b -> send(new RequestKytNameListPayload(entityId)))
                    .bounds(centerX - 75, y + 24, 150, 20).build());
        } else {
            nameBox = addRenderableWidget(new EditBox(font, centerX - 100, y, 200, 20, Component.literal("Kyt name")));
            nameBox.setMaxLength(48);

            addRenderableWidget(Button.builder(Component.literal("Confirm"), b -> confirmCreate())
                    .bounds(centerX - 100, y + 24, 95, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> {
                creating = false;
                rebuildWidgets();
            }).bounds(centerX + 5, y + 24, 95, 20).build());
        }
    }

    private void confirmCreate() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        send(new RequestCreateKytPayload(entityId, name));
    }

    private void send(CustomPacketPayload payload) {
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(payload));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int titleWidth = font.width(title);
        graphics.text(font, title, width / 2 - titleWidth / 2, 30, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
