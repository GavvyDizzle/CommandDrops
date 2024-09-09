# Rewards
Documentation for reward usage and configuration

# Overview
- The plugin relies on the relationship between `pools`, `entries`, `attempts`, and `rolls`
- A `pool` has an `activation_chance` and one to many entries. Think of this as the loot table
- An `entry` has a `weight`. This determines how common it is pulled from the loot table 
- A valid action will trigger an `attempt` to a pool
- A successful attempt will choose an `entry` from the pool

## Reward Algorithm
1. On any valid action, a `pool` `attempt` is triggered
2. Checks that the pool is not on cooldown and the player meets its permissions
3. Triggers a `AttemptRewardRollEvent` to allow plugins to alter the `activation_chance`
4. Randomly determine if the pool should `roll`
5. A roll chooses a random `entry` from its entry set. Every roll produces an entry
6. A chosen entry will be discarded if on cooldown or the player does not meet its permissions
7. Finally, the `commands` for the entry will be run for the player

### Algorithm Notes
- The `simulate` commands allow for sending multiple attempts or rolls at once
  - These actions are batched, meaning they only check pool permissions and cooldowns once
  - Entries are still triggered one by one to respect cooldowns. This means permissions and cooldowns are checked multiple times
    - Use the `simulateIgnoreCooldown` command to bypass any cooldown checks and setting
- A `pool` on cooldown will NOT fire a `AttemptRewardRollEvent`
- An `entry` on cooldown will fail silently. It can still be chosen from a `roll`
  - This behavior is necessary to keep the loot table weights equivalent when some of the `entries` are on cooldown
- Any cooldown stuff mentioned above can be ignored if the cooldown module is disabled

## Reward Types
- `MINING` Activates when players break any block
  - Trigger: `BlockBreakEvent`
- `FISHING` Activates when players catch a fish
  - Trigger: `PlayerFishEvent` when an item is generated (`PlayerFishEvent.State.CAUGHT_FISH`)
- `FARMING` Activates when players break any fully grown crop
  - Trigger: `BlockBreakEvent` when a fully grown crop is broken

### Warning
- The `MINING` and `FARMING` types can both activate on the farming criteria. Refrain from using both on the same pool!

## Additional Notes
- Due to database constraints, the pool ID and entry ID have some restrictions
- Both ID types are limited to 16 characters. (regex is `[\w-]{1,16}`)
- The pool ID is pulled from the filename e.g. `my_pool.yml` -> `my_pool`
- The entry ID is pulled from the parent key in the config
- Take care not modify pool or entry ID values after they are in use (if using cooldowns)!

# Pool Configuration
```yml
# Supported types have associated WorldGuard flags of the format cmddrops_<type>
types:
  - FISHING
  - FARMING
  - MINING
# The chance for the pool to roll. A number between 0 and 1 inclusive
activation_chance: 1
# Permissions required to roll this pool
permissions: []
# Lock rolling this pool for some time after any successful roll
# Setting a cooldown to min=0 max=0 will disable it
# Supports time in seconds or the format 1d2h5m30s
cooldown:
  player:
    min: 60s
    max: 2m
  global:
    min: 30s
    max: 60s

# Entry keys are used in commands and the cooldown database. Consider naming them well
entries:
  '1':
    # This entry's weight in the entire pool
    weight: 4.0
    # Only used in the admin menu: /drops list
    menu_material: STONE
    # Commands to run. {player_name} is replaced with the player's name
    commands:
      - eco give {player_name} 500
    # Permissions needed to receive this entry's rewards
    permissions: []
    # Lock rewards from this entry for some time after a successful roll
    cooldown:
      player:
        min: 5m
        max: 10m
      global:
        min: 0
        max: 0
  '2':
    weight: 1.0
    menu_material: DIAMOND
    commands:
      - minecraft:give {player_name} diamond 1
      - su msg {player_name} &ewoah!
    permissions: []
    cooldown:
      player:
        min: 5m
        max: 10m
      global:
        min: 0
        max: 0
```