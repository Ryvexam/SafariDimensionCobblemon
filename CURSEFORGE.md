# Safari Dimension (Cobblemon)

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-3C8527?style=for-the-badge)
![Loader](https://img.shields.io/badge/Loader-Fabric-DBD0B4?style=for-the-badge)
![Cobblemon](https://img.shields.io/badge/Cobblemon-1.7.1-1E6FB8?style=for-the-badge)
![Version](https://img.shields.io/badge/Safari%20Dimension-0.0.14-F28C28?style=for-the-badge)

A dedicated Safari experience for Cobblemon with portal entry, timed sessions, economy integration, curated spawn pools, and Safari-style capture rules.

Bug reports and support: https://discord.gg/zxZXcaTHwe

## What this mod adds

- A separate Safari dimension with custom biome + controlled gameplay rules.
- A custom portal frame and ignition flow (Nether-style frame, lit with flint and steel).
- Safari session system (timed entry, reconnect resume, safe return handling).
- Safari economy loop (entry fee, shop purchases, time extensions, tickets).
- Safari Ball-focused capture flow with rarity-based catch tuning.
- Curated Cobblemon spawn pools by rarity bucket.
- Fishing support in Safari pools.
- Shiny odds in Safari synced to Cobblemon global `shinyRate`.
- Client/server mod version handshake for compatibility checks.

## How it works

1. **Enter Safari**
   - Player enters via portal or `/safari enter`.
   - Entry checks run (money, inventory, session state).
2. **Start Session**
   - Timer starts, player is placed in Safari, and gameplay restrictions apply.
   - If player disconnects, session is paused and can resume on reconnect.
3. **Catch and Progress**
   - Safari Ball usage is enforced inside Safari.
   - Catch rates are tuned by spawn rarity bucket.
   - Spawn pools define what appears in each tier (including fishing entries).
4. **Exit / Timeout**
   - Leaving or timer end returns player safely to previous location.
   - Illegal/non-session access is automatically corrected.

## Gameplay rules inside Safari

- No Pokemon send-out battles.
- Any unexpected battle in Safari is ended automatically.
- Block breaking/placing restricted (creative exceptions for admin/building use).
- Only Safari-allowed ball usage behavior.
- Day/night stays aligned with Overworld time.

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

World config path:
`<world>/safari-config.json`

Main tuning fields:
- `commonCatchRate`, `uncommonCatchRate`, `rareCatchRate`, `ultraRareCatchRate`
- `safariMinLevel`, `safariMaxLevel`
- `entrancePrice`
- `pack16BallsPrice`, `pack32BallsPrice`, `pack64BallsPrice`
- `timePurchaseMinutes`, `timePurchasePrice`
- `forceCustomSpawn`, `customSpawnX`, `customSpawnY`, `customSpawnZ`
- `safariSpawnY`, `safariSpawnOffsetY`
- `dimensionSize`

## Dependencies

- Fabric API
- Cobblemon 1.7.1
- Cobblemon Economy 0.0.15

## Credits

- Safari Portal NPC contribution: Aerendil
