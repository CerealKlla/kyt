package com.github.cerealklla.kyt;

import java.util.Optional;

import com.github.cerealklla.kyt.structure.OpenKytMainMenuPayload;
import com.github.cerealklla.kyt.structure.OpenKytPickerPayload;
import com.github.cerealklla.nuclyus.request.ClientRequestSlot;

/**
 * Client-side bridge for "a Kyt screen should open" -- zero-client-import poll-once-handoff
 * pattern, same shape as Settlemynts' {@code guardhouse.ClientGuardhouseRequests}/{@code
 * plotsign.ClientPlotSignRequests}. Payload handlers (registered in {@code KytMod#registerPayloads},
 * which runs on both sides) just stash the payload here; a client-tick subscriber (see {@code
 * KytModClient}) polls and actually opens the {@code Screen} -- keeps this class, and the common
 * registration method that calls it, free of any client-only import.
 *
 * <p>The editor screen itself does <b>not</b> go through this bridge -- it opens via vanilla's own
 * menu-open packet (see {@code loadout.KytEditorMenuProvider}), which the client handles
 * automatically through {@code RegisterMenuScreensEvent}'s registered factory.
 */
public final class ClientKytRequests {

    private static final ClientRequestSlot<OpenKytMainMenuPayload> MAIN_MENU = new ClientRequestSlot<>();
    private static final ClientRequestSlot<OpenKytPickerPayload> PICKER = new ClientRequestSlot<>();

    private ClientKytRequests() {
    }

    public static void requestMainMenu(OpenKytMainMenuPayload payload) {
        MAIN_MENU.request(payload);
    }

    public static Optional<OpenKytMainMenuPayload> takePendingMainMenu() {
        return MAIN_MENU.takePending();
    }

    public static void requestPicker(OpenKytPickerPayload payload) {
        PICKER.request(payload);
    }

    public static Optional<OpenKytPickerPayload> takePendingPicker() {
        return PICKER.takePending();
    }
}
