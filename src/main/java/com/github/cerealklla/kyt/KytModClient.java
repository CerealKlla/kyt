package com.github.cerealklla.kyt;

import com.github.cerealklla.kyt.loadout.client.LoadoutEditorScreen;
import com.github.cerealklla.kyt.registration.ModEntities;
import com.github.cerealklla.kyt.registration.ModMenus;
import com.github.cerealklla.kyt.structure.client.KytConfigurationRenderer;
import com.github.cerealklla.kyt.structure.client.KytMainMenuScreen;
import com.github.cerealklla.kyt.structure.client.KytPickerScreen;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = KytMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = KytMod.MODID, value = Dist.CLIENT)
public class KytModClient {

    public KytModClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.KYT_CONFIGURATION.get(), KytConfigurationRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.LOADOUT.get(), LoadoutEditorScreen::new);
    }

    // Polls the zero-server-refs bridge (see ClientKytRequests' own doc) for a pending screen-open
    // request every client tick -- same pattern as Settlemynts' SettlemyntsModClient. The editor
    // screen itself isn't polled here -- it opens automatically via vanilla's own menu-open packet.
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        ClientKytRequests.takePendingMainMenu().ifPresent(request -> {
            if (Minecraft.getInstance().screen == null) {
                Minecraft.getInstance().setScreen(new KytMainMenuScreen(request));
            }
        });
        ClientKytRequests.takePendingPicker().ifPresent(request ->
                // Unlike the Main Menu request (which fires from right-clicking the structure in
                // world, with no screen open yet), this one always fires from a button click inside
                // the already-open KytMainMenuScreen -- gating on `screen == null` here meant this
                // branch could never actually fire ("Edit Kyt" silently did nothing). Always replace
                // the current screen with the picker.
                Minecraft.getInstance().setScreen(new KytPickerScreen(request)));
    }
}
