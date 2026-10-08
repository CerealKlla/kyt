package com.github.cerealklla.kyt;

import com.github.cerealklla.kyt.api.Kyt;
import com.github.cerealklla.kyt.debug.DebugCommands;
import com.github.cerealklla.kyt.loadout.KytEditorMenuProvider;
import com.github.cerealklla.kyt.loadout.LoadoutMenu;
import com.github.cerealklla.kyt.loadout.LoadoutRecord;
import com.github.cerealklla.kyt.loadout.LoadoutStorage;
import com.github.cerealklla.kyt.loadout.RequestCreateKytPayload;
import com.github.cerealklla.kyt.loadout.RequestEditKytPayload;
import com.github.cerealklla.kyt.loadout.RequestPaletteItemPayload;
import com.github.cerealklla.kyt.loadout.SaveKytPayload;
import com.github.cerealklla.kyt.registration.ModAttachments;
import com.github.cerealklla.kyt.registration.ModEntities;
import com.github.cerealklla.kyt.registration.ModItems;
import com.github.cerealklla.kyt.registration.ModMenus;
import com.github.cerealklla.kyt.structure.OpenKytMainMenuPayload;
import com.github.cerealklla.kyt.structure.OpenKytPickerPayload;
import com.github.cerealklla.kyt.structure.RequestKytNameListPayload;
import com.github.cerealklla.kyt.system.SystemKyts;
import com.github.cerealklla.kyt.welcome.WelcomeListener;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(KytMod.MODID)
public class KytMod {

    public static final String MODID = "kyt";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KytMod(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModMenus.MENU_TYPES.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);

        // SystemKyts.bootstrap() runs at ServerStartingEvent, not FMLCommonSetupEvent -- it must
        // come after every sibling mod's constructor has had a chance to call
        // api.Kyt#registerDevKytContribution (see system.DevKytContributions' own doc for why).
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> SystemKyts.bootstrap());
        NeoForge.EVENT_BUS.register(new WelcomeListener());
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> DebugCommands.register(event.getDispatcher()));
    }

    private void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("Kyt common setup");
    }

    /** See {@code LoadoutRecord}'s own doc -- a {@code systemManaged} Kyt can't be created, edited, or overwritten through this mod's normal player-facing UI. */
    private static boolean isSystemManaged(String name) {
        return Kyt.getKyt(name).map(LoadoutRecord::systemManaged).orElse(false);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        // Server-to-client: hand off to the client-only bridge, never touch Screen/Minecraft here
        // (this method runs on both sides -- see ClientKytRequests' own doc). The editor screen
        // itself opens through vanilla's own menu-open packet instead (see KytEditorMenuProvider),
        // not through a payload at all.
        registrar.playToClient(OpenKytMainMenuPayload.TYPE, OpenKytMainMenuPayload.STREAM_CODEC,
                (payload, context) -> ClientKytRequests.requestMainMenu(payload));
        registrar.playToClient(OpenKytPickerPayload.TYPE, OpenKytPickerPayload.STREAM_CODEC,
                (payload, context) -> ClientKytRequests.requestPicker(payload));

        registrar.playToServer(RequestKytNameListPayload.TYPE, RequestKytNameListPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }
                    PacketDistributor.sendToPlayer(player, new OpenKytPickerPayload(payload.entityId(), Kyt.listKytNames()));
                });

        registrar.playToServer(RequestCreateKytPayload.TYPE, RequestCreateKytPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }
                    String name = sanitizedName(payload.name());
                    if (name.isEmpty()) {
                        player.sendSystemMessage(Component.literal("Kyt name can't be blank."));
                        return;
                    }
                    if (isSystemManaged(name)) {
                        player.sendSystemMessage(Component.literal("That Kyt is managed by the server and can't be created, edited, or overwritten."));
                        return;
                    }
                    LoadoutRecord current = LoadoutStorage.get().load(name).orElse(LoadoutRecord.empty(name));
                    player.openMenu(new KytEditorMenuProvider(name, true, current));
                });

        registrar.playToServer(RequestEditKytPayload.TYPE, RequestEditKytPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }
                    if (isSystemManaged(payload.name())) {
                        player.sendSystemMessage(Component.literal("That Kyt is managed by the server and can't be created, edited, or overwritten."));
                        return;
                    }
                    LoadoutStorage.get().load(payload.name()).ifPresentOrElse(
                            current -> player.openMenu(new KytEditorMenuProvider(payload.name(), false, current)),
                            () -> player.sendSystemMessage(Component.literal("That Kyt no longer exists.")));
                });

        registrar.playToServer(RequestPaletteItemPayload.TYPE, RequestPaletteItemPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }
                    if (!(player.containerMenu instanceof LoadoutMenu menu)) {
                        return;
                    }
                    Item item = BuiltInRegistries.ITEM.getOptional(payload.itemId()).orElse(null);
                    if (item == null) {
                        return;
                    }
                    int count = Math.max(1, Math.min(payload.count(), 64));
                    menu.setCarried(new ItemStack(item, count));
                    menu.broadcastChanges();
                });

        registrar.playToServer(SaveKytPayload.TYPE, SaveKytPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }
                    if (!(player.containerMenu instanceof LoadoutMenu menu)) {
                        return;
                    }
                    String name = sanitizedName(menu.isNameEditable() ? payload.name() : menu.name());
                    if (name.isEmpty()) {
                        player.sendSystemMessage(Component.literal("Kyt name can't be blank."));
                        return;
                    }
                    if (isSystemManaged(name)) {
                        player.sendSystemMessage(Component.literal("That Kyt is managed by the server and can't be created, edited, or overwritten."));
                        return;
                    }
                    if (menu.isNameEditable() && !name.equals(menu.name())) {
                        // Renamed mid-edit -- drop the old file so we don't leave a stale duplicate
                        // behind under the original name.
                        LoadoutStorage.get().delete(menu.name());
                    }
                    LoadoutStorage.get().save(menu.toRecord(name));
                    player.sendSystemMessage(Component.literal("Saved Kyt \"" + name + "\"."));
                });
    }

    private static String sanitizedName(String name) {
        return name == null ? "" : name.trim();
    }
}
