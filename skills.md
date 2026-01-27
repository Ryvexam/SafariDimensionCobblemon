# SafariCobblemond AI Skills

This document lists the skills and system knowledge expected from an AI agent working on this mod, with a focus on reliability and scalability.

## Core technical skills
- Java 21 and Fabric mod lifecycle (entrypoints, registries, events, server hooks).
- Mixin authoring and maintenance, including remap=false for Cobblemon targets.
- Data pack editing for dimensions, biomes, loot, and spawn pools.
- Reflection patterns for Cobblemon and Economy integrations.
- Server-side gameplay rule enforcement and safe teleportation patterns.

## System knowledge map (must know)
- Entry + lifecycle: `src/main/java/com/safari/SafariMod.java`
- Client entrypoint: `src/main/java/com/safari/client/SafariClientMod.java`
- Sessions: `src/main/java/com/safari/session/SafariSessionManager.java`, `src/main/java/com/safari/session/SafariSession.java`
- Config: `src/main/java/com/safari/config/SafariConfig.java`, world file `<world>/safari-config.json`
- World state: `src/main/java/com/safari/state/SafariWorldState.java`, runtime file `config/safari-state.json`
- Worldgen: `src/main/java/com/safari/world/SafariDimension.java`, `src/main/java/com/safari/world/SafariChunkGenerator.java`
- Events: `src/main/java/com/safari/events/SafariEvents.java`
- Economy: `src/main/java/com/safari/economy/SafariEconomy.java`
- Capture tuning: `src/main/java/com/safari/logic/SafariSpawnRarity.java`, `src/main/java/com/safari/mixin/MixinPokeBallCaptureCalculatedEvent.java`
- Commands: `src/main/java/com/safari/command/SafariCommand.java`
- Portal: `src/main/java/com/safari/block/SafariPortalBlock.java`
- NPCs + shop: `src/main/java/com/safari/entity/SafariNpcEntity.java`, `src/main/java/com/safari/shop/SafariShopScreenHandler.java`
- Data resources: `src/main/resources/data/safari/dimension/safari.json`, `src/main/resources/data/cobblemon/spawn_pool_world/safari/*.json`
- Mod metadata/mixins: `src/main/resources/fabric.mod.json`, `src/main/resources/safari.mixins.json`

## Operational flow knowledge
- Session entry path: `/safari enter` or `SafariPortalBlock` -> `SafariSessionManager.tryStartSession` -> `startSession`.
- Tick loop behavior: sync Safari time, action bar timer, expire sessions, expel illegal players.
- Persistence: paused sessions in `safari-sessions.json`, world state in `safari-state.json`, economy log in `safari-transactions.log`.

## Reliability guardrails (skills to apply)
- Keep config keys stable; normalize defaults and never crash on load.
- Maintain safe return positions and portal avoidance.
- Keep Safari restrictions: no damage, no attacks, no send-out, only Safari balls.
- Preserve worldgen determinism via `currentDailySeed`.
- Ensure economy failures are non-fatal and logs remain intact.
- Update translations when adding or changing message keys.

## Scalability practices
- Avoid per-tick reflection or heavy loops; prefer event hooks and cached values.
- Favor data-driven changes (spawn pools, config) over hard-coded logic.
- Limit log volume to state transitions only.
- Keep session and player tracking in bounded, well-scoped maps.

## Safe change workflow
- Read relevant files first; keep diffs small and localized.
- Update config defaults + normalization when adding new fields.
- Update `safari.mixins.json` when adding mixins or changing targets.
- Ensure new resources are registered or referenced by data packs.
- Validate with `./gradlew clean build` before shipping.
