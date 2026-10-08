package com.github.cerealklla.kyt.registration;

import java.util.function.Supplier;

import com.github.cerealklla.kyt.KytMod;
import com.github.cerealklla.kyt.welcome.WelcomeGrantState;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Kyt's first attachment registry (added 2026-10-02, see decisions.md) -- previously Kyt had no
 * per-player data at all, since loadouts themselves are global/shared storage, not per-player.
 * Mirrors Lyfe's own {@code registration.ModAttachments} shape exactly.
 */
public final class ModAttachments {

    private ModAttachments() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, KytMod.MODID);

    // Not synced -- server-only bookkeeping, nothing client-side reads it. copyOnDeath() is
    // required, not optional: NeoForge attachments do NOT survive a player's death by default (a
    // brand-new Player entity is created on respawn), so skipping this would re-grant the Welcome
    // Kyt on every death, not just true first-join -- see Lyfe's own ModAttachments javadoc for
    // the fuller account of this exact bug class.
    public static final Supplier<AttachmentType<WelcomeGrantState>> WELCOME_GRANT_STATE = ATTACHMENT_TYPES.register(
            "welcome_grant_state",
            () -> AttachmentType.builder(holder -> WelcomeGrantState.DEFAULT)
                    .serialize(WelcomeGrantState.CODEC)
                    .copyOnDeath()
                    .build()
    );
}
