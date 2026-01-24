# Safari Dimension Mod

Custom Minecraft 1.21.1 Fabric mod for Cobblemon that adds a dedicated Safari dimension with portal access, economy integration, and Safari‑style captures.

## Features

- **Safari Dimension** with a custom Safari biome (sparse jungle style).
- **Portal frame** block (Nether‑style size detection) lit by flint & steel.
- **Safari Ball item** (`safari:safari_ball`) with Cobblemon throw physics and sounds.
- **No send‑out** inside Safari (prevents battles).
- **No damage** inside Safari; block breaking/placing is **creative‑only**.
- **Only Safari Balls** can be used in Safari.
- **Safari Guide NPC** with a shop for balls and time tickets, auto‑spawns near `(0,0)`.
- **Custom Safari spawn pools** (common/uncommon/rare/ultra‑rare + special pools).
- **Session resume** on reconnect with remaining time.
- **Entry warning** title that leaving ends the session.
- **Shop UI** shows your Pokédollar balance and offers 16/32/64 ball bundles.
- **World border** 2000x2000 centered on `(0,0)`.
- **World‑specific config** stored in each world save.

## Dependencies

Place these jars in your `mods` folder:
1. `fabric-api`
2. `cobblemon` (1.7.1)
3. `cobblemon-economy`

## Configuration

World‑specific config file:
`world/<your-world>/safari-config.json`

Default values:
```json
{
  "sessionTimeMinutes": 30,
  "initialSafariBalls": 25,
  "safariBallItem": "safari:safari_ball",
  "carryOverSafariBalls": false,
  "logoutClearInventory": true,
  "allowMultiplayerSessions": true,
  "entrancePrice": 2500,
  "pack16BallsPrice": 400,
  "pack32BallsPrice": 750,
  "pack64BallsPrice": 1400,
  "maxBallsPurchasable": 20,
  "commonCatchRate": 0.45,
  "uncommonCatchRate": 0.18,
  "rareCatchRate": 0.1,
  "dimensionSize": 2000,
  "coreRadius": 350,
  "resetOffsetRange": 100000,
  "safariSpawnY": 160,
  "safariSpawnOffsetY": 3,
  "allowedBiomes": [
    "safari:safari_biome"
  ],
  "spawnRateMultiplier": 1.5,
  "safariMinLevel": 5,
  "safariMaxLevel": 30,
  "timePurchaseMinutes": 30,
  "timePurchasePrice": 1000,
  "starterBoostRadius": 120,
  "starterCullChance": 0.45,
  "starterSpecies": [
    "bulbasaur",
    "charmander",
    "squirtle"
  ]
}
```

## Portal

1. Build a Nether‑style frame with `safari:safari_portal_frame`.
2. Light it with flint & steel.
3. Walk into the portal to start a Safari session.

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
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean build
```

Jar output:
`build/libs/safari-dimension-0.0.2.jar`

## Notes

- The Safari spawn is always centered at `(0, 0)`.
- Players need at least one empty inventory slot to enter (Safari Balls are added).
