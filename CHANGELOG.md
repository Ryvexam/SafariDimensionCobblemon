# Changelog

## 0.0.10
- Updated Safari Ball 3D model to use `cobblemon:item/poke_ball_model` as parent.
- Removed logic that gives free Safari Balls on entry.
- Removed logic that clears Safari Balls on session end.

## 0.0.9
- Fixed Safari Ball 3D model texture reference using correct Cobblemon path.

## 0.0.8
- Updated dependency to Cobblemon Economy 0.0.11.
- Fixed Safari Ball 3D model texture reference.
- Removed experimental world border isolation logic to prevent startup crashes with other mods.

## 0.0.7
- Synchronize Safari dimension time with the Overworld time.
- Remove fixed time in Safari dimension type.
- Hard block portal entry if the player cannot afford the entrance price.
- Fix Safari Guide NPC failing to respawn if previously killed or missing.
- Add 3D render for Safari Ball when held in hand.
- Fix ConcurrentModificationException when teleporting players out of Safari.
- Prevent non-op players from teleporting into the Safari dimension without a session.
- Bounce non-op players back to their last known non-Safari position if they attempt to enter illegally.
- Increase Safari NPC tracking range to 48 blocks for better visibility.
- NPCs are no longer pushable by players or other entities.
- Added configurable custom spawn point (`forceCustomSpawn`, `customSpawnX`, `customSpawnY`, `customSpawnZ`).
- Disabled automatic spawning of the Safari Guide NPC (manual spawn required).
- World border is now automatically set and centered on the spawn point.
- Corrected entity spawn blocking to only target natural vanilla spawns, allowing manual NPC spawning.

## 0.0.6
- Persist Safari sessions across disconnects and server shutdowns.
- Prevent non-session players from staying in the Safari dimension.
- Allow NPCs to be killed via `/kill` while keeping them damage-immune otherwise.
- Fix NPC spawn egg usage and despawn behavior in the Safari dimension.
- Add full translation coverage (EN/FR) for player messages and UI.
- Add configurable entry title text and timing.
- Add biome/dimension language keys for minimap and F3 display.
- Add ticket, shop, and action bar localization keys.
- Ensure portal entry uses the same checks as `/safari enter`.
- End Safari sessions immediately on player death.

## 0.0.5
- Initial public build.
