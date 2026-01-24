# Safari Dimension (Cobblemon)

Dedicated Safari dimension for Cobblemon 1.7.1 (MC 1.21.1).

## Features

- Custom Safari biome (sparse jungle style).
- Nether‑style portal frame (custom block) + flint & steel ignition.
- Safari Ball item with Cobblemon throw physics and sounds.
- No send‑out inside Safari (prevents battles).
- No damage + no block breaking/placing inside Safari (creative only for building).
- Only Safari Balls can be used in Safari.
- Safari Guide NPC with shop for balls and time tickets, auto-spawns near (0,0).
- Session resumes on reconnect with remaining time.
- Entry warning title reminds you time is lost on exit.
- Safari Ball catch rate follows spawn rarity buckets.
- Shop UI shows your Pokédollar balance and 16/32/64 ball bundles.
- World border 2000x2000 centered at (0,0).

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
- `commonCatchRate`, `uncommonCatchRate`, `rareCatchRate`
- `ultraRareCatchRate`
- `safariSpawnY`, `safariSpawnOffsetY`
- `safariMinLevel`, `safariMaxLevel`
- `pack16BallsPrice`, `pack32BallsPrice`, `pack64BallsPrice`
- `timePurchaseMinutes`, `timePurchasePrice`

## Dependencies

- Fabric API
- Cobblemon 1.7.1
- Cobblemon Economy
