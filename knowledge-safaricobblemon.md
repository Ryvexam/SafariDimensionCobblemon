# Safari Ticket Bot Knowledge Base

Purpose: help support/ticket responses for the Safari Dimension mod and guide players/admins to resolve common issues quickly.

## Quick overview
- Mod: Safari Dimension for Fabric + Cobblemon (1.21.1)
- Main features: dedicated Safari dimension, Safari portal, Safari balls only, timed sessions, economy-based entry and purchases.
- Safari sessions end immediately when the player leaves the dimension (teleport, logout, death, etc.).

## Dependencies
Required jars in `mods/`:
- `fabric-api`
- `cobblemon` (1.7.1)
- `cobblemon-economy` (0.0.13)

If economy features fail (entry, tickets, ball purchases), confirm `cobblemon-economy` is installed and loaded.

## Config
World config file (auto-created on first load):
- `<world>/safari-config.json`

Key settings used by tickets and sessions:
- `sessionTimeMinutes`: default 30
- `timePurchaseMinutes`: default 30
- `timePurchasePrice`: default 1000
- `entrancePrice`: default 2500
- `initialSafariBalls`: default 25
- `maxBallsPurchasable`: default 20

Note: If values are 0 or negative, the mod normalizes them back to defaults on load.

## Player commands
Base command: `/safari`

Session:
- `/safari enter` (charges `entrancePrice`, requires 1 empty inventory slot)
- `/safari leave`
- `/safari info`

Purchases:
- `/safari buy balls 16|32|64`
- `/safari buy time <minutes>`
- `/safari buy ticket <minutes>` (valid minutes: 5, 15, 30)

Admin:
- `/safari reload` (permission level 2)

## Ticket items
Ticket items (used to add time during a session):
- `safari:ticket_5` (+5 minutes)
- `safari:ticket_15` (+15 minutes)
- `safari:ticket_30` (+30 minutes)

Rules enforced by the mod:
- Tickets can only be used inside the Safari dimension.
- Tickets require an active Safari session.
- Tickets are on a short cooldown (20 ticks) to avoid double-use.
- On success, the ticket is consumed and time is added.

Related error messages players may see:
- "You can only use this in the Safari Zone."
- "You are not in a Safari session."

## Common issues and fixes
### 1) Player cannot enter Safari
Symptoms:
- "Your inventory is full! You need 1 slot for Safari Balls before entering Safari."
- "You need <price> Pokédollars to enter!"
- "Error: Safari Dimension not loaded! Contact Admin."

Resolutions:
- Ask the player to clear one inventory slot.
- Verify their balance in Cobblemon economy, or lower `entrancePrice` in config.
- Restart the server or confirm the dimension datapack is present and `safari` dimension is registered.

### 2) Ticket purchase fails
Symptoms:
- "Invalid ticket. Use 5, 15, or 30."
- "You need <price> Pokédollars to buy this ticket!"

Resolutions:
- Only minutes 5, 15, 30 are supported by `/safari buy ticket <minutes>`.
- Confirm player balance and `timePurchaseMinutes`/`timePurchasePrice` config values.

### 3) Ticket use fails
Symptoms:
- "You can only use this in the Safari Zone."
- "You are not in a Safari session."

Resolutions:
- The player must be inside the Safari dimension and actively in-session. If they left the dimension, the session ends automatically.

### 4) Cannot throw balls in Safari
Symptoms:
- "Only Safari Balls can be used here."

Resolutions:
- Only `safari:safari_ball` is permitted. The mod blocks Cobblemon balls in Safari.
- Ensure the player received Safari balls on entry, and that their inventory has space.

### 5) Player loses Safari balls on exit
Expected behavior:
- Safari balls are removed automatically when a session ends.
- Inventory is not cleared; only Safari balls are removed.

### 6) Economy transactions look wrong
Where to check:
- Server log: info entries like "Safari economy deduct" show balance before/after.
- `<world>/safari-transactions.log` records timestamps and balances for each transaction.

## Portal troubleshooting
Portal lighting requirements:
- Build a Nether-style rectangular frame using `safari:safari_portal_frame`.
- Use Flint & Steel on a frame block.
- Frame must be complete and rectangular (min 4x5, max 23x23 including frame).

If the portal will not light:
- Confirm the interior is air (no blocks inside).
- Confirm the frame size is within limits.
- Confirm the frame blocks are the Safari portal frame, not vanilla obsidian.

## Session behavior reminders
- Leaving the Safari dimension ends the session immediately.
- Sessions resume on reconnect only if the player is still in the Safari dimension.
- A session requires at least one empty inventory slot on entry (for Safari balls).

## Useful file paths
- Config: `<world>/safari-config.json`
- Economy log: `<world>/safari-transactions.log`
- Safari inventories (used for save/restore in some flows): `<run_dir>/safari_inventories/<uuid>.dat`

## What to collect for a bug report
- Player name and UUID
- Exact error message text (copy/paste)
- Whether the player is in the Safari dimension at the time
- Server log snippet around the error
- Current `safari-config.json` values
