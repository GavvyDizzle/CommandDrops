package me.github.gavvydizzle.commanddrops.cooldown;

import me.github.gavvydizzle.commanddrops.pool.RewardPool;
import me.github.gavvydizzle.commanddrops.pool.entry.RewardEntry;

import java.util.HashMap;
import java.util.Map;

public class GlobalCooldown extends AbstractCooldown {

    public GlobalCooldown() {
        super(new HashMap<>(), new HashMap<>());
    }

    public GlobalCooldown(Map<String, Long> poolUnlockTimes, Map<String, Long> entryUnlockTimes) {
        super(poolUnlockTimes, entryUnlockTimes);
    }

    @Override
    int getRandomCooldownLength(RewardPool pool) {
        return pool.getRandomGlobalCooldown();
    }

    @Override
    int getRandomCooldownLength(RewardEntry entry) {
        return entry.getRandomGlobalCooldown();
    }

    public void setData(Map<String, Long> poolUnlockTimes, Map<String, Long> entryUnlockTimes) {
        clear();
        this.poolUnlockTimes.putAll(poolUnlockTimes);
        this.entryUnlockTimes.putAll(entryUnlockTimes);
    }

    public void clear() {
        poolUnlockTimes.clear();
        entryUnlockTimes.clear();
    }
}
