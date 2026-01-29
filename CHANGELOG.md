# Changelog

## 0.0.12
- **Bug Fix:** Missed Safari Balls now correctly drop as the modded item (`safari:safari_ball`) instead of the standard Cobblemon one.
- **Battle Restriction:** Prevented battles from being initiated within the Safari Zone (no more launching Pokémon at wild ones). Added a fallback to immediately end any battle that somehow starts in the Safari dimension.
- **Session Security:** Players found in the Safari dimension without an active session (and not an OP) are now automatically teleported back to their last known safe position or world spawn.
- **NPC AI Update:** Safari Guide and Safari Keeper are now stationary and will smoothly look at nearby players.
- **Permissions:** Restricted NPC renaming with Name Tags to Operators (level 2+) only.
- **Localization:** Added full translation support for all new messages and visibility modes.
- **Handshake:** Clients must match the server mod version on join; mismatches are disconnected with a clear message.
- **Gameplay:** Boats can be broken in the Safari (other entity attacks remain blocked).
- **Fix:** `/safari reload` no longer corrupts the config path on repeated reloads.
- **Maintenance:** Updated dependency to Cobblemon Economy 0.0.13.

## 0.0.10
- Added NPC Name Toggler feature (`/safari npcnametoggler`) to cycle NPC name visibility (Hover, Always, Never).
- Updated Safari Guide and Safari Portal NPC to support name visibility persistence.

## 0.0.9
- Implemented Hybrid 2D/3D Safari Ball rendering (2D icon in GUI/hotbar, 3D model in hand).
- Fixed Safari Ball 3D model texture using `cobblemon:item/poke_ball_model` parent and official textures.
- Inventory logic update: no free balls on entry, no clearing balls on exit (players must buy balls in shop).
- Updated dependency to Cobblemon Economy 0.0.11.
- Removed experimental world border isolation logic to prevent startup crashes with other mods.

## 0.0.8
- (Consolidated into 0.0.9)

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
