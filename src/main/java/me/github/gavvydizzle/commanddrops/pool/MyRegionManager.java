package me.github.gavvydizzle.commanddrops.pool;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.EnumMap;

public class MyRegionManager {

    private static EnumMap<ActivationType, StateFlag> flagsMap;
    private RegionQuery query;

    public static void initFlags() {
        flagsMap = new EnumMap<>(ActivationType.class);

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        for (ActivationType activationType : ActivationType.values()) {
            if (activationType.regionFlagName == null) continue;

            try {
                StateFlag flag = new StateFlag(activationType.regionFlagName, false);
                registry.register(flag);
                flagsMap.put(activationType, flag);
            }  catch (IllegalStateException | FlagConflictException e) {
                // If the plugin is reloaded dynamically then this will grab the existing flag
                // ... or another plugin registered it already
                Flag<?> existing = registry.get(activationType.regionFlagName);
                if (existing instanceof StateFlag) {
                    flagsMap.put(activationType, (StateFlag) existing);
                }
            }
        }
    }

    private void initQuery() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        query = container.createQuery();
    }

    public boolean isNotInRegion(ActivationType activationType, Entity entity) {
        return isNotInRegion(activationType, entity.getLocation());
    }

    public boolean isNotInRegion(ActivationType activationType, Block block) {
        return isNotInRegion(activationType, block.getLocation());
    }

    public boolean isNotInRegion(ActivationType activationType, Location location) {
        if (query == null) {
            initQuery();
        }

        StateFlag stateFlag = flagsMap.get(activationType);
        if (activationType == null) return false;

        return !query.testState(BukkitAdapter.adapt(location), null, stateFlag);
    }
}