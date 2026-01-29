# AI Agent Guide for SafariCobblemond

Purpose: enable safe, predictable changes to the Safari Dimension mod. This document is the ground truth for how the code behaves today and how to extend it without breaking gameplay.

## How the mod works (ground truth)
- **Boot and lifecycle**: `SafariMod` registers SERVER_STARTING to load `safari-config.json`, set noise params for `SafariChunkGenerator`, set the server for sessions, and load `SafariWorldState`. SERVER_STOPPING persists sessions to `safari-sessions.json`.
- **Session entry**: Entry is via `/safari enter` or `SafariPortalBlock` collision. `SafariSessionManager.tryStartSession` checks inventory space and economy, then `startSession` validates the Safari dimension, ensures a safe spawn via `SafariWorldManager`, records a safe return position, and teleports the player. Entry titles and protection effects are read from config.
- **Session runtime**: `SafariSessionManager.tick` runs every server tick, syncs Safari time to Overworld, updates the action bar timer each second, ends expired sessions, and expels non-op players found in Safari without a session using `lastKnownPositions`.
- **Session exit and persistence**: `endSession` teleports the player back (safe spot away from portal), with fallback to Overworld spawn. Disconnect pauses sessions and reconnect resumes them. Active sessions are persisted on shutdown.
- **Rule enforcement**: `SafariEvents` blocks non-creative block breaking and most block placing in Safari, blocks attacks, and ends battles every tick. `UseItemCallback` blocks non-Safari balls. Mixins prevent damage (`MixinLivingEntityDamage`) and block send-out (`MixinSendOutPokemonHandler`).
- **Capture tuning**: `MixinPokeBallCaptureCalculatedEvent` overrides capture results in Safari using rarity buckets from `SafariSpawnRarity`, which reads Cobblemon spawn pools under `data/cobblemon/spawn_pool_world/safari/*.json`.
- **Worldgen and spawn**: `SafariChunkGenerator` delegates to vanilla noise with a daily seed from `SafariWorldState.currentDailySeed`. `SafariWorldManager` ensures spawn/border and validates allowed biomes from config.
- **Economy**: `SafariEconomy` uses reflection to call Cobblemon Economy. It fails closed when reflection breaks and logs deductions to `safari-transactions.log`.
- **NPCs and shop**: `SafariNpcEntity` opens `SafariShopScreenHandler` for balls and tickets. `SafariPortalNpcEntity` runs the `safari enter` command. Both use Name Toggler (custom named feather) and restrict renaming to OPs.
- **Data and resources**: Dimension JSON in `src/main/resources/data/safari/dimension/safari.json`, spawn pools in `src/main/resources/data/cobblemon/spawn_pool_world/safari/`, mixins in `src/main/resources/safari.mixins.json`, and translations in `src/main/resources/assets/safari/lang/*.json`.
- **Optional compat**: YAWP integration allows NPC damage if the flag is explicitly allowed.

## Reliability invariants (must keep)
- Players without active sessions must not remain in the Safari dimension (non-op should be expelled).
- No damage, no attacks, no send-out, and no battles in Safari.
- Session end must return players to a safe location and avoid portal loops.
- Config load must never crash the server; defaults and normalization must be safe.
- Worldgen must remain deterministic via `SafariWorldState.currentDailySeed`.
- Economy integration must fail gracefully and keep logging intact.
- Registry IDs for items, blocks, entities, and dimension must remain stable.

## Change checklist
- If you create a commit: include the current working version in the commit message (e.g., "[0.0.11] ...").
- Default workflow expectation: commit and push changes after completion. If tooling rules require explicit user confirmation to commit/push, ask for that confirmation before doing so.
- Use one branch per feature, named `feature/xxxxxxx`.
- If you add config fields: update defaults in `SafariConfig`, normalize in `normalizeDefaults`, and ensure rewrites are safe.
- If you add player-facing text: update `src/main/resources/assets/safari/lang/en_us.json` and `src/main/resources/assets/safari/lang/fr_fr.json`.
- If you add mixins: update `src/main/resources/safari.mixins.json` and verify remap settings for Cobblemon targets.
- If you add session state: update `SafariSession`, pause/resume records, and persistence in `SafariSessionManager`.
- If you add data resources: ensure they are referenced by the dimension or registries.

## Scalability and performance
- Avoid new per-tick loops or reflection in hot paths. Prefer event callbacks and cached values.
- Keep logging at info level for state transitions only, not per tick.
- Favor data-driven tuning (spawn pools, config) over hard-coded logic.
- Use existing cooldowns and guards (portal cooldown, enter cooldown) to prevent spam.

## Known sharp edges
- `SafariCaptureLogic` exists but is not wired into capture flow; capture tuning currently happens only via `MixinPokeBallCaptureCalculatedEvent`.
- `SafariInventoryHandler.saveAndClear/restore` are unused; only `giveSafariKit` is called.
- `MixinPokemonEntity` exists but is not enabled in `safari.mixins.json`.

## Build and verify
- Local build: `./gradlew clean build`

## Quick file map
- Entry + lifecycle: `src/main/java/com/safari/SafariMod.java`
- Sessions: `src/main/java/com/safari/session/SafariSessionManager.java`
- Events: `src/main/java/com/safari/events/SafariEvents.java`
- Economy: `src/main/java/com/safari/economy/SafariEconomy.java`
- Worldgen: `src/main/java/com/safari/world/SafariChunkGenerator.java`
- World state: `src/main/java/com/safari/state/SafariWorldState.java`
- Commands: `src/main/java/com/safari/command/SafariCommand.java`
- Portal block: `src/main/java/com/safari/block/SafariPortalBlock.java`
- NPCs + shop: `src/main/java/com/safari/entity/SafariNpcEntity.java`, `src/main/java/com/safari/shop/SafariShopScreenHandler.java`
- Mixins config: `src/main/resources/safari.mixins.json`
