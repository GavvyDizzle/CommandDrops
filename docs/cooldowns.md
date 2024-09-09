# Cooldowns
Set real-time cooldowns on desired rewards

# Enabling
- The cooldown module is entirely optional
- Setting `cooldowns_enabled: true` in `config.yml` will enable this module
  - Can be enabled in-game with the `/drops reload` command
  - Be aware! This module *requires* a database connection. It will cause the server to hang if the connection is invalid during a reload!

# Usage
- Each `pool` and `entry` can be put on cooldown
- Cooldowns can be set per player and/or globally (all players)
- Global and player cooldowns are saved separately, so both can be used at the same time!
  - Using both only makes sense if the player cooldown is longer than the global cooldown

## Behavior
- A pool is put on cooldown if a player successfully rolls an entry
- An entry is put on cooldown if a player successfully rolls that specific entry
- A pool on `global` cooldown will block any attempt for all players
- A pool on `player` cooldown will block any attempt for only that player
- An entry on `global` cooldown will not run commands for this entry when rolled for all players
- An entry on `player` cooldown will not run commands for this entry when rolled for only that player

## Configuration
- If the `min` and `max` values differ, a random duration will be chosen within the range
```yml
# A subsection for a pool or entry
# Setting a cooldown to min=0 max=0 will disable it
# Supports time in seconds or the format 1d2h5m30s
cooldown:
  player:
    min: 60s
    max: 2m
  global:
    min: 30s
    max: 60s
```