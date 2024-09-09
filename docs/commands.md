# Commands
This plugin only has admin commands

## Admin Commands
- The base permission is `commanddrops.admin`
- All commands require permission to use which follows the format `commanddrops.admin.command` where command is the name of the command
- `/drops help` Opens the help menu
- `/drops list` Opens the reward pool list menu
- `/drops purgeDatabase` Purge the database of unused pool and entry references
- `/drops reload` Reloads all data
- `/drops simulate <type> <poolID> <player> <amount>` Simulate reward events
- `/drops simulateIgnoreCooldown <type> <poolID> <player> <amount>` Simulate reward events while ignoring all cooldown logic
- `/drops testEntry <player> <entryID>` Test pool entries
