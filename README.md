# CommandDrops
A command-based rewards plugin

# Features
- Create unlimited loot tables to reward players by running commands
- Supports rewards when mining, fishing, or farming
- Each reward type has its own WorldGuard flag: `cmddrops-<type>`
- Rewards can be put on a real-time global or per-player cooldown
- Admin menu system to view each pool (can edit weights in-game)

# Dependencies
- [ServerUtils](https://github.com/MittenMC/ServerUtils) 1.1.9+
- [WorldGuard](https://dev.bukkit.org/projects/worldguard)

# Database
- The cooldown module requires a MySQL Database
  - The cooldown module of this plugin is optional (and disabled by default)