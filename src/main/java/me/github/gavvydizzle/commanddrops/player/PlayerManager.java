package me.github.gavvydizzle.commanddrops.player;

import com.github.mittenmc.serverutils.player.PlayerDataContainer;
import me.github.gavvydizzle.commanddrops.CommandDrops;
import me.github.gavvydizzle.commanddrops.data.PlayerCooldownDatabase;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

public class PlayerManager extends PlayerDataContainer<LoadedPlayer> {

    private final CommandDrops instance;

    public PlayerManager(CommandDrops instance) {
        super(instance);
        this.instance = instance;

        super.initializeOnlinePlayers();
    }

    /**
     * Reloads data for all online players.
     * This will initiate a call to {@link #loadPlayerData(Player)} for all online players.
     */
    public void refreshData() {
        super.initializeOnlinePlayers();
    }

    @Override
    public @Nullable LoadedPlayer loadPlayerData(Player player) {
        if (CommandDrops.areCooldownsDisabled()) {
            return new LoadedPlayer(player);
        }

        try {
            PlayerCooldownDatabase database = instance.getPlayerCooldownDatabase();
            if (database != null) {
                return instance.getPlayerCooldownDatabase().loadPlayerData(player);
            }
        } catch (SQLException ignored) {}

        return null;
    }

    @Override
    public @Nullable LoadedPlayer loadOfflinePlayerData(OfflinePlayer offlinePlayer) {
        return null;
    }

    @Override
    public void savePlayerData(LoadedPlayer loadedPlayer) {
        if (CommandDrops.areCooldownsDisabled()) return;

        PlayerCooldownDatabase database = instance.getPlayerCooldownDatabase();
        if (database != null) {
            instance.getPlayerCooldownDatabase().savePlayerData(loadedPlayer);
        }
    }

    @Override
    public void saveAllPlayerData() {
        if (CommandDrops.areCooldownsDisabled()) return;

        PlayerCooldownDatabase database = instance.getPlayerCooldownDatabase();
        if (database != null) {
            instance.getPlayerCooldownDatabase().savePlayerData(super.getAllPlayerData());
        }
    }
}
