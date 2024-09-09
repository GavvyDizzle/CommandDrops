package me.github.gavvydizzle.commanddrops.cooldown;

import me.github.gavvydizzle.commanddrops.pool.RewardPool;
import me.github.gavvydizzle.commanddrops.pool.entry.RewardEntry;

import java.util.Map;

public class PlayerCooldown extends AbstractCooldown {

    public PlayerCooldown(Map<String, Long> poolUnlockTimes, Map<String, Long> entryUnlockTimes) {
        super(poolUnlockTimes, entryUnlockTimes);
    }

    @Override
    int getRandomCooldownLength(RewardPool pool) {
        return pool.getRandomPlayerCooldown();
    }

    @Override
    int getRandomCooldownLength(RewardEntry entry) {
        return entry.getRandomPlayerCooldown();
    }
}
