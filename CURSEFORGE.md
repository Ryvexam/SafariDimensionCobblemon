# Safari Dimension (Cobblemon)

Dedicated Safari dimension for Cobblemon 1.7.1 (MC 1.21.1).
any bugs report on :  https://discord.gg/zxZXcaTHwe
## Features

- Custom Safari biome (sparse jungle style).
- Nether‑style portal frame (custom block) + flint & steel ignition.
- Safari Ball item with Cobblemon throw physics and sounds.
- 3D render for Safari Ball when held in hand.
- No send‑out inside Safari (prevents battles).
- No damage + no block breaking/placing inside Safari (creative only for building).
- Only Safari Balls can be used in Safari.
- Day/Night cycle synchronized with the Overworld.
- Safari Guide NPC with shop for balls and time tickets.
- NPCs are non-pushable by players or entities.
- Thanks Aerendil for the Safari Portal NPC.
- Session resumes on reconnect with remaining time.
- Entry warning title reminds you time is lost on exit.
- Hard block portal entry if the player cannot afford the entrance price.
- Safari Ball catch rate follows spawn rarity buckets.
- Shop UI shows your Pokédollar balance and 16/32/64 ball bundles.
- World border centered on spawn point (configurable size).
- Illegal entries (non-op) are automatically returned to their last position.

## Commands

- `/safari enter`
- `/safari leave`
- `/safari info`
- `/safari buy balls 16`
- `/safari buy balls 32`
- `/safari buy balls 64`
- `/safari buy time <minutes>`
- `/safari buy ticket <minutes>`
- `/safari reload` (admin)

## Config

World config file:
`<world>/safari-config.json`

Key fields:
- `commonCatchRate`, `uncommonCatchRate`, `rareCatchRate`, `ultraRareCatchRate`
- `forceCustomSpawn`, `customSpawnX`, `customSpawnY`, `customSpawnZ`
- `safariSpawnY`, `safariSpawnOffsetY`
- `safariMinLevel`, `safariMaxLevel`
- `pack16BallsPrice`, `pack32BallsPrice`, `pack64BallsPrice`
- `timePurchaseMinutes`, `timePurchasePrice`
- `dimensionSize`

## Dependencies

- Fabric API
- Cobblemon 1.7.1
- Cobblemon Economy
