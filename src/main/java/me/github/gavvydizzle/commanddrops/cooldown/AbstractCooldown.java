package me.github.gavvydizzle.commanddrops.cooldown;

import lombok.Getter;
import me.github.gavvydizzle.commanddrops.pool.RewardPool;
import me.github.gavvydizzle.commanddrops.pool.entry.RewardEntry;

import java.util.Collection;
import java.util.Map;

@Getter
public abstract class AbstractCooldown {

    protected final Map<String, Long> poolUnlockTimes, entryUnlockTimes;

    protected AbstractCooldown(Map<String, Long> poolUnlockTimes, Map<String, Long> entryUnlockTimes) {
        this.poolUnlockTimes = poolUnlockTimes;
        this.entryUnlockTimes = entryUnlockTimes;
    }

    public void removePools(Collection<String> poolIDs) {
        for (String str : poolIDs) {
            poolUnlockTimes.remove(str);
        }
    }

    public void removeEntries(Collection<String> entryIDs) {
        for (String str : entryIDs) {
            entryUnlockTimes.remove(str);
        }
    }

    abstract int getRandomCooldownLength(RewardPool pool);

    abstract int getRandomCooldownLength(RewardEntry entry);

    public void placeOnCooldown(RewardPool pool) {
        int rand = getRandomCooldownLength(pool);
        if (rand <= 0) return;

        long unlockTimeMillis = System.currentTimeMillis() + rand * 1000L;
        poolUnlockTimes.put(pool.getId(), unlockTimeMillis);
    }

    public void placeOnCooldown(RewardEntry entry) {
        int rand = getRandomCooldownLength(entry);
        if (rand <= 0) return;

        long unlockTimeMillis = System.currentTimeMillis() + rand * 1000L;
        entryUnlockTimes.put(entry.getId(), unlockTimeMillis);
    }

    public boolean onCooldown(RewardPool pool) {
        long now = System.currentTimeMillis();
        return poolUnlockTimes.getOrDefault(pool.getId(), 0L) > now;
    }

    public boolean onCooldown(RewardEntry entry) {
        long now = System.currentTimeMillis();
        return entryUnlockTimes.getOrDefault(entry.getId(), 0L) > now;
    }
}
