package me.github.gavvydizzle.commanddrops;

import com.github.mittenmc.serverutils.autosave.AutoSaver;
import com.github.mittenmc.serverutils.database.DatabaseConnectionPool;
import lombok.Getter;
import me.github.gavvydizzle.commanddrops.command.AdminCommandManager;
import me.github.gavvydizzle.commanddrops.data.GlobalCooldownDatabase;
import me.github.gavvydizzle.commanddrops.data.PlayerCooldownDatabase;
import me.github.gavvydizzle.commanddrops.gui.InventoryManager;
import me.github.gavvydizzle.commanddrops.player.PlayerManager;
import me.github.gavvydizzle.commanddrops.pool.MyRegionManager;
import me.github.gavvydizzle.commanddrops.pool.RewardManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public final class CommandDrops extends JavaPlugin {

    @Getter private static CommandDrops instance;
    @Getter private PlayerManager playerManager;
    @Getter private RewardManager rewardManager;
    @Getter private InventoryManager inventoryManager;
    private AutoSaver autoSaver;

    private static boolean areCooldownsEnabled;
    private DatabaseConnectionPool databaseConnectionPool;
    @Getter @Nullable
    private GlobalCooldownDatabase globalCooldownDatabase;
    @Getter @Nullable
    private PlayerCooldownDatabase playerCooldownDatabase;

    @Override
    public void onLoad() {
        reload();

        if (areCooldownsEnabled) {
            initializeDatabase();
        }

        MyRegionManager.initFlags();
    }

    public void reload() {
        boolean oldCooldownsEnabled = areCooldownsEnabled;

        FileConfiguration config = super.getConfig();
        config.options().copyDefaults(true);

        config.options().copyDefaults(true);
        config.addDefault("cooldowns_enabled", false);
        areCooldownsEnabled = config.getBoolean("cooldowns_enabled");

        super.saveConfig();

        // Update the database state if the value changed
        if (areCooldownsEnabled != oldCooldownsEnabled) {
            if (areCooldownsEnabled) {
                initializeDatabase();
            } else {
                closeDatabaseConnections();
            }
        }
    }

    private void initializeDatabase() {
        databaseConnectionPool = new DatabaseConnectionPool(this);
        if (!databaseConnectionPool.testConnection()) {
            getLogger().severe("Unable to connect to database. Disabling cooldown module!");
            areCooldownsEnabled = false;
            return;
        }
        globalCooldownDatabase = new GlobalCooldownDatabase(databaseConnectionPool);
        playerCooldownDatabase = new PlayerCooldownDatabase(databaseConnectionPool);

        if (playerManager != null) {
            playerManager.refreshData();
        }
        if (rewardManager != null) {
            rewardManager.refreshData();
        }
    }

    private void closeDatabaseConnections() {
        if (databaseConnectionPool != null) {

            if (globalCooldownDatabase != null && rewardManager != null) {
                try {
                    globalCooldownDatabase.saveData(rewardManager.getGlobalCooldown());
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to save global cooldown data on server shutdown. Data since the last auto save will be lost.", e);
                }
            }

            if (playerManager != null) {
                try {
                    playerManager.saveAllPlayerData();
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to save player data on server shutdown. Data since the last auto save will be lost for online players.", e);
                }
            }

            databaseConnectionPool.close();

            databaseConnectionPool = null;
            playerCooldownDatabase = null;
            globalCooldownDatabase = null;
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        playerManager = new PlayerManager(this);
        rewardManager = new RewardManager(this, playerManager);
        inventoryManager = new InventoryManager(this, rewardManager);

        // Must call after the inventory manager has been created
        rewardManager.reload();

        new AdminCommandManager(getCommand("drops"), this, playerManager, rewardManager, inventoryManager);

        autoSaver = new AutoSaver(this, AutoSaver.TimeUnit.FIVE_MINUTES) {
            @Override
            public void save() {
                if (areCooldownsEnabled) {
                    if (playerManager != null){
                        playerManager.saveAllPlayerData();
                    }
                    if (globalCooldownDatabase != null && rewardManager != null) {
                        globalCooldownDatabase.saveData(rewardManager.getGlobalCooldown());
                    }
                }
            }
        };
    }

    @Override
    public void onDisable() {
        if (autoSaver != null) autoSaver.cancel();

        closeDatabaseConnections();
    }

    public static boolean areCooldownsEnabled() {
        return areCooldownsEnabled;
    }

    public static boolean areCooldownsDisabled() {
        return !areCooldownsEnabled;
    }
}
