# Kyt

Named equipment-loadout data mod for a modular Minecraft project (Minecraft Java Edition). Part of
a suite of intercommunicating mods that expose public APIs so other mods — the user's own and third
parties' — can integrate.

## Context directory — read this first

`context/` is a **separate private repo** (https://github.com/CerealKlla/kyt-context), not part of
this one — it's listed in `.gitignore` here and must never be committed to this repo. It's cloned as
a subdirectory at `context/` for local convenience. If this directory is missing (e.g. a fresh clone
of just this repo), restore it with:

```
git clone https://github.com/CerealKlla/kyt-context.git context
```

Before searching source for architecture, ownership boundaries, API shape, or "why does this work
this way," check `context/` first. It's maintained specifically to answer those questions cheaply:

- `context/design-document.md` — the authoritative design spec. Start here for anything about
  intended shape or scope.
- `context/decisions.md` — dated log of decisions made during implementation that extend or
  override the design document, with rationale. Check this for anything that looks like it
  contradicts design-document.md — the doc should already reflect the current decision, but this
  explains why.
- `context/classes/` — one short markdown file per implemented class: public surface, key state,
  collaborators. Read the relevant file here before opening the actual source, and before editing a
  class update its file to match.

**Keep this system current as you work:**
- When a design decision is made that conflicts with or is absent from design-document.md, update
  design-document.md directly and add a dated entry to decisions.md explaining the change.
- When a class is added or its public surface changes, add or update its file in `context/classes/`.
- Don't let source and these docs drift — treat updating them as part of finishing the change, not
  optional cleanup.
- `context/` has its own git history, independent of this repo's commits. Commit and push changes
  there separately (`git -C context add . && git -C context commit -m "..." && git -C context
  push`) — editing the files alone doesn't back them up.

## Status

Scaffolded 2026-10-01 (see [context/decisions.md](context/decisions.md)):
- Loader: **NeoForge**
- Minecraft version: **26.1.2**
- Java: **JDK 25** — same toolchain as the rest of the suite
- Group ID: `com.github.cerealklla.kyt` / Mod ID: `kyt`
- **No sibling-mod dependencies** — Kyt stands alone; other mods (Settlemynts) depend on it
  instead, via the suite's usual `compileOnly` sibling-jar pattern.
- Project structure copied from Protectyons' own MDK setup (`build.gradle`, `settings.gradle`,
  wrapper).

**Phase 1 built 2026-10-01** (see [context/decisions.md](context/decisions.md)) — the "Kyt
Configuration" structure (a disguised marker entity rendered as a vanilla Armor Stand), its Main
Menu -> Create/Edit GUI, a real inventory-shaped Create/Edit screen (Helmet/Chestplate/Leggings/
Boots/Offhand/Hotbar, type-restricted armor slots), a from-scratch creative-tab-style item palette,
global JSON storage (`LoadoutStorage`), and the read-only `api.Kyt` facade. `./gradlew build` green
alone and whole-suite (`sync-mods.sh`), boot-smoke-tested clean via `runServer` from Cartographyr's
project. **Live client confirmation (placing the structure, exercising the GUI/palette/save-load
loop) is still pending.**

**Phase 2 (Settlemynts wiring), 2026-10-01**: "Edit Kyt"'s name field is confirmed **read-only**,
matching the original spec — a same-day attempt to make it editable was a misreading of user
feedback and was reverted. (The rename-on-save safety net in `KytMod#registerPayloads`'s
`SaveKytPayload` handler — delete the old file if the saved name differs from `menu.name()` — was
kept since it's harmless/dead in Edit mode and still a reasonable guard for Create mode.) Phase 2
built:
Settlemynts' `GarrisonSlot` gained an `Optional<String> kytLoadoutName` field, its
`ConfigureGarrisonScreen` gained a per-slot "Select Kyt" button (visible only when
`bridge.KytGuardhouseBridge.isLoaded()`) opening a new `client.SelectKytScreen` paged picker —
selecting a name or Clear sends `SetGarrisonKytPayload` and returns to a client-rebuilt Configure
Garrison screen. `./gradlew build` green in both repos, whole-suite rebuild green.

**Live-tested and confirmed, 2026-10-01**, across several rounds of real playtesting:
- "Edit Kyt" opens the picker (fixed a bug where the client-tick handler gated the picker open on
  `screen == null`, which could never be true since the request always fires from inside the
  already-open Main Menu screen — `KytModClient` now always swaps to it) and the name field stays
  read-only, as intended.
- Typing in the Kyt Name box no longer closes the screen (the `keyPressed` override intercepting
  non-Escape keys while the name box is focused).
- Configure Garrison's "Select Kyt" row no longer runs off the right edge of the screen (centered
  row layout, column count now varies correctly with `kytAvailable`).
- `LoadoutEditorScreen`'s visual polish is done: header (title + name box) left-aligned with the
  panel, the offhand slot moved up beside the mannequin instead of dangling by its feet, no more
  leftover strip of vanilla's real inventory-row texture bleeding in below the armor column, the
  hotbar row's background fully backed edge to edge, the dead gap between the armor panel and
  hotbar closed (panel pushed down 14px, hotbar pulled up from vanilla's 142 to 110 — both purely
  destination-coordinate changes, texture sampling stayed pinned to its real source position), and
  the leftover vanilla "Inventory" label (`AbstractContainerScreen#extractLabels`' own
  auto-rendered `playerInventoryTitle`) suppressed. User confirmation: "the Kyt config UIs look way
  better now. All 3 look good now."

Phase 1 and Phase 2 are both now considered live-confirmed and done for this slice.

**`Kyt.getKyt(name)` now has a real caller, 2026-10-01** — Settlemynts' Guardhouse garrison
guard-spawning system (see `settlemynts-context`'s own decisions.md for the full build) reads a
bound garrison slot's Kyt name via `bridge.KytGuardhouseBridge#getKyt` and equips a spawned
`PathfinderMob` directly from the returned `LoadoutRecord` — helmet/chestplate/leggings/boots/
offhand from their matching fields, mainhand from the first non-empty hotbar stack. This closes the
one open gap this file used to flag here. Kyt's own code was untouched for this — the consumer-side
work lives entirely in Settlemynts.

**Mod-managed "Dev"/"Welcome" Kyts; suite-wide debug item grants removed, 2026-10-02** (see
[context/decisions.md](context/decisions.md), [context/classes/SystemKyts.md](context/classes/SystemKyts.md),
[LoadoutApplier.md](context/classes/LoadoutApplier.md), [Welcome.md](context/classes/Welcome.md),
[DebugCommands.md](context/classes/DebugCommands.md)) — `LoadoutRecord` gained `systemManaged` and
a 27-slot `inventory` overflow; `/kyt getDev` (Kyt's first-ever command) grants a consolidated "Dev"
Kyt replacing the old unconditional-on-login debug grants that used to live in Lyfe/Settlemynts/
Blueprynts (all three removed the same day); a new "Welcome" Kyt (a dynamically-generated "Kyngdoms,
A History" book, one section per currently-loaded mod) is auto-granted exactly once on a player's
true first login (Kyt's first-ever attachment registry). Both Kyts are invisible to every
player-facing UI and protected from create/edit/overwrite. Since Kyt has zero sibling-mod
dependencies by design, the five Lyfe/one Settlemynts/one Blueprynts custom debug items reach the
Dev Kyt via a new contribution registry (`api.Kyt#registerDevKytContribution`) rather than Kyt
depending on them directly. A real crash (an enchanted item can't be saved through this mod's plain-
`JsonOps` storage layer) was found and fixed before any live test even ran. `./gradlew build`/`test`
green across all four repos, whole-suite rebuild green. **Confirmed live, 2026-10-02** — "all looked
good enough to move on for now."

See [context/classes/](context/classes/) for per-class reference.
