# Safari Dimension Mod

Custom Minecraft 1.21.1 Fabric mod for Cobblemon that adds a dedicated Safari dimension with portal access, economy integration, and Safari‑style captures.

## Features

- **Safari Dimension** with a custom Safari biome (sparse jungle style).
- **Portal frame** block (Nether‑style size detection) lit by flint & steel.
- **Safari Ball item** (`safari:safari_ball`) with Cobblemon throw physics and sounds.
- **No send‑out** inside Safari (prevents battles).
- **Auto-end battles** if they are somehow triggered in the Safari dimension.
- **No damage** inside Safari; block breaking/placing is **creative‑only**.
- **Only Safari Balls** can be used in Safari.
- **Safari Guide NPC** with a shop for balls and time tickets.
- **Safari Portal NPC** (thanks Aerendil).
- **Custom Safari spawn pools** (common/uncommon/rare/ultra‑rare + special pools).
- **Safari Ball catch rate** follows spawn rarity (common→ultra‑rare).
- **Session resume** on reconnect with remaining time.
- **Entry warning** title that leaving ends the session.
- **Day/Night synchronization** with the Overworld.
- **3D Render** for Safari Balls when held in hand.
- **Shop UI** shows your Pokédollar balance and offers 16/32/64 ball bundles.
- **Stationary NPCs** that look at nearby players instead of wandering.
- **OP-only NPC renaming** to prevent unauthorized changes.
- **World border** configurable size, centered on spawn point.
- **Config file** stored in the main `config` folder.

## Dependencies

Place these jars in your `mods` folder:
1. `fabric-api`
2. `cobblemon` (1.7.1)
3. `cobblemon-economy` (0.0.13)

## Configuration

World config file:
`<world>/safari-config.json`

Default values:
```json
{
  "sessionTimeMinutes": 30,
  "initialSafariBalls": 25,
  "carryOverSafariBalls": false,
  "entrancePrice": 2500,
  "pack16BallsPrice": 400,
  "pack32BallsPrice": 750,
  "pack64BallsPrice": 1400,
  "maxBallsPurchasable": 20,
  "commonCatchRate": 0.45,
  "uncommonCatchRate": 0.18,
  "rareCatchRate": 0.1,
  "ultraRareCatchRate": 0.05,
  "dimensionSize": 2000,
  "forceCustomSpawn": false,
  "customSpawnX": 0.5,
  "customSpawnY": 160.0,
  "customSpawnZ": 0.5,
  "safariSpawnY": 160,
  "safariSpawnOffsetY": 3,
  "allowedBiomes": [
    "safari:safari_biome"
  ],
  "safariMinLevel": 5,
  "safariMaxLevel": 30,
  "timePurchaseMinutes": 30,
  "timePurchasePrice": 1000
}
```

## Portal

1. Build a Nether‑style frame with `safari:safari_portal_frame`.
2. Light it with flint & steel.
3. Walk into the portal to start a Safari session.
   * **Note:** You must have enough money to enter (default 2500), otherwise you will be pushed back.

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

## Building

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk"
./gradlew clean build
```

Jar output:
`build/libs/safari-dimension-0.0.12.jar`

## Git workflow

- Create one branch per feature using the `feature/xxxxxxx` naming format.

## Notes

- The Safari spawn is centered at `(0, 0)` or the custom coordinates if enabled.
- Players need at least one empty inventory slot to enter (Safari Balls are added).
- NPCs are not pushable by players or entities.
- Non-op players are returned to their last known position if they enter without a session.
