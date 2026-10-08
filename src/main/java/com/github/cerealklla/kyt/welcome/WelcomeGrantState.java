package com.github.cerealklla.kyt.welcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-player flag: has this player ever been granted the "Welcome" Kyt? (added 2026-10-02, see
 * decisions.md). Server-only bookkeeping -- not synced, nothing client-side reads it, same
 * reasoning as Lyfe's own {@code knowledge.PlayerKnowledge}. Schema-versioned for consistency with
 * every other attachment in this suite, even though a single boolean barely needs it.
 */
public record WelcomeGrantState(boolean granted) {

    public static final int SCHEMA_VERSION = 1;

    public static final MapCodec<WelcomeGrantState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            com.mojang.serialization.Codec.BOOL.fieldOf("granted").forGetter(WelcomeGrantState::granted)
    ).apply(i, WelcomeGrantState::new));

    public static final WelcomeGrantState DEFAULT = new WelcomeGrantState(false);
}
