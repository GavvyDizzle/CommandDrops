package me.github.gavvydizzle.commanddrops.pool;

import com.github.mittenmc.serverutils.gui.filesystem.tree.ItemFileNode;
import com.github.mittenmc.serverutils.gui.filesystem.tree.Node;
import lombok.Getter;
import me.github.gavvydizzle.commanddrops.CommandDrops;
import me.github.gavvydizzle.commanddrops.cooldown.GlobalCooldown;
import me.github.gavvydizzle.commanddrops.data.GlobalCooldownDatabase;
import me.github.gavvydizzle.commanddrops.gui.filesystem.PoolItemFileNode;
import me.github.gavvydizzle.commanddrops.gui.filesystem.SortableDataNode;
import me.github.gavvydizzle.commanddrops.player.LoadedPlayer;
import me.github.gavvydizzle.commanddrops.player.PlayerManager;
import me.github.gavvydizzle.commanddrops.pool.entry.RewardEntry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RewardManager implements Listener {

    private final static Set<Material> CROP_MATERIALS = Tag.CROPS.getValues();
    private final Pattern poolFileNamePattern = Pattern.compile("[\\w-]{1,16}");

    private final CommandDrops instance;
    private final PlayerManager playerManager;
    private final MyRegionManager regionManager;
    private final File dataFolder;
    private final Map<String, RewardPool> poolMap;
    private final EnumMap<ActivationType, List<RewardPool>> poolByTypeMap;
    private Node root;

    @Getter @NotNull
    private final GlobalCooldown globalCooldown;

    public RewardManager(CommandDrops instance, PlayerManager playerManager) {
        this.instance = instance;
        instance.getServer().getPluginManager().registerEvents(this, instance);
        this.dataFolder = new File(instance.getDataFolder(), "pools");

        this.playerManager = playerManager;
        poolMap = new HashMap<>();
        poolByTypeMap = new EnumMap<>(ActivationType.class);

        regionManager = new MyRegionManager();

        globalCooldown = new GlobalCooldown();
        refreshData();
    }

    public void refreshData() {
        if (CommandDrops.areCooldownsDisabled()) {
            globalCooldown.clear();
        } else {
            GlobalCooldownDatabase database = instance.getGlobalCooldownDatabase();
            if (database != null) {
                Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
                    try {
                        // Must keep the original cooldown reference alive
                        GlobalCooldown db = database.loadData();
                        Bukkit.getScheduler().runTask(instance, () -> {
                            if (db != null) {
                                globalCooldown.setData(db.getPoolUnlockTimes(), db.getEntryUnlockTimes());
                            } else {
                                globalCooldown.clear();
                            }
                        });
                    } catch (Exception ignored) {
                        globalCooldown.clear();
                    }
                });
            }
        }
    }

    public void reload() {
        poolMap.clear();
        poolByTypeMap.clear();
        root = new ItemFileNode(null, "root", true);
        parseRecursively(dataFolder);
    }

    /**
     * Recursively parses all .yml in this folder
     * @param folder The root folder
     */
    private void parseRecursively(File folder) {
        if (folder.isFile()) {
            throw new RuntimeException("Provided file is not a folder");
        }

        //noinspection ResultOfMethodCallIgnored
        folder.mkdirs();

        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            parseFiles(folder, root);
            instance.getInventoryManager().refreshFileSystemMenus();
        });
    }

    private void parseFiles(File folder, Node curr) {
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            String fileName = fileEntry.getName();

            if (fileEntry.isDirectory()) {
                ItemFileNode itemFileNode = new ItemFileNode(curr, fileName, true);
                curr.add(itemFileNode);
                parseFiles(fileEntry, itemFileNode);
            }
            else if (fileName.endsWith(".yml")) {
                readFile(fileEntry, curr);
            }
        }
    }

    private void readFile(File file, Node curr) {
        Matcher matcher = poolFileNamePattern.matcher(file.getName().replace(".yml", ""));
        if (!matcher.matches()) {
            instance.getLogger().warning("The pool file '" + file.getName() + "' has an invalid name. Ensure it is 16 characters or less (regex: " + poolFileNamePattern.pattern() + ")");
            return;
        }

        RewardPool pool = new RewardPool(file, globalCooldown);
        poolMap.put(pool.getId(), pool);

        pool.getActivationTypes().forEach(type -> {
            List<RewardPool> poolList = poolByTypeMap.getOrDefault(type, new ArrayList<>());
            poolList.add(pool);
            poolByTypeMap.put(type, poolList);
        });

        PoolItemFileNode itemFileNode = new PoolItemFileNode(curr, pool, false);
        curr.add(itemFileNode);

        for (RewardEntry entry : pool.getEntries()) {
            itemFileNode.add(new SortableDataNode<>(curr, entry.getId(), entry));
        }
    }

    private void handleRewardAttempt(Event e, Player player, ActivationType activationType) {
        LoadedPlayer lp = playerManager.getPlayerData(player);
        if (lp == null) return;

        poolByTypeMap.getOrDefault(activationType, List.of()).forEach(pool -> {
            if (pool.canAttempt(activationType, lp)) {
                pool.attempt(e, lp, activationType);
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBlockMine(BlockBreakEvent e) {
        if (e.getBlock().hasMetadata("player_placed")) return; // Ignore player placed blocks in mines
        else if (regionManager.isNotInRegion(ActivationType.MINING, e.getBlock())) return;

        handleRewardAttempt(e, e.getPlayer(), ActivationType.MINING);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onCropHarvest(BlockBreakEvent e) {
        if (isNotCrop(e.getBlock().getType())) return;
        else if (!isFullyGrown(e.getBlock().getState())) return;
        else if (regionManager.isNotInRegion(ActivationType.FARMING, e.getBlock())) return;

        handleRewardAttempt(e, e.getPlayer(), ActivationType.FARMING);
    }

    private boolean isNotCrop(Material material) {
        return !CROP_MATERIALS.contains(material);
    }

    private boolean isFullyGrown(BlockState state) {
        if (state.getBlockData() instanceof Ageable ageable) {
            int grown = ageable.getMaximumAge();
            return ageable.getAge() == grown && ageable.getAge() != 0;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onFishCatch(PlayerFishEvent e) {
        Entity caughtEntity = e.getCaught();
        if (caughtEntity == null || e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        else if (regionManager.isNotInRegion(ActivationType.FISHING, e.getPlayer())) return;

        handleRewardAttempt(e, e.getPlayer(), ActivationType.FISHING);
    }


    @Nullable
    public RewardPool getPool(String id) {
        return poolMap.get(id.toLowerCase());
    }

    public Collection<String> getPoolIDs() {
        return poolMap.keySet();
    }

    public Collection<RewardPool> getPools() {
        return poolMap.values();
    }

    public Node getRootNode() {
        return root;
    }
}
