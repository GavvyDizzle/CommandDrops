package me.github.gavvydizzle.commanddrops.data;

import com.github.mittenmc.serverutils.database.Database;
import com.github.mittenmc.serverutils.database.DatabaseConnectionPool;
import me.github.gavvydizzle.commanddrops.cooldown.PlayerCooldown;
import me.github.gavvydizzle.commanddrops.player.LoadedPlayer;
import me.github.gavvydizzle.commanddrops.pool.RewardManager;
import me.github.gavvydizzle.commanddrops.pool.RewardPool;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerCooldownDatabase extends Database {

    private static final String poolTableName = "player_pool_cooldowns";
    private static final String LOAD_POOL_DATA = "SELECT * FROM " + poolTableName + " WHERE uuid=?";
    private static final String SAVE_POOL_DATA = "INSERT INTO " + poolTableName + " (uuid,pool_id,time_available) VALUES(?,?,?) ON DUPLICATE KEY UPDATE time_available = VALUES(time_available)";
    private static final String DELETE_POOL_ENTRY = "DELETE FROM " + poolTableName + " WHERE pool_id=?";
    private static final String GET_ALL_POOL_IDS_POOL = "SELECT DISTINCT pool_id FROM " + poolTableName;

    private static final String entryTableName = "player_entry_cooldowns";
    private static final String LOAD_ENTRY_DATA = "SELECT * FROM " + entryTableName + " WHERE uuid=?";
    private static final String SAVE_ENTRY_DATA = "INSERT INTO " + entryTableName + " (uuid,pool_id,entry_id,time_available) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE time_available = VALUES(time_available)";
    private static final String DELETE_POOL_ENTRIES = "DELETE FROM " + entryTableName + " WHERE pool_id=?";
    private static final String DELETE_ENTRY_ENTRY = "DELETE FROM " + entryTableName + " WHERE pool_id=? AND entry_id=?";
    private static final String GET_ALL_POOL_IDS_ENTRY = "SELECT DISTINCT pool_id, entry_id FROM " + entryTableName;

    public PlayerCooldownDatabase(DatabaseConnectionPool pool) {
        super(pool);
    }

    @Nullable
    public LoadedPlayer loadPlayerData(OfflinePlayer player) throws SQLException {
        Map<String, Long> poolUnlockTimes = new HashMap<>();
        Map<String, Long> entryUnlockTimes = new HashMap<>();

        ResultSet rs = null;

        try (Connection connection = pool.getConnection();
             PreparedStatement s1 = connection.prepareStatement(LOAD_POOL_DATA);
             PreparedStatement s2 = connection.prepareStatement(LOAD_ENTRY_DATA)) {

            s1.setString(1, player.getUniqueId().toString());
            rs = s1.executeQuery();
            while (rs.next()) {
                String poolID = rs.getString("pool_id");
                long time = rs.getLong("time_available");
                poolUnlockTimes.put(poolID, time);
            }

            s2.setString(1, player.getUniqueId().toString());
            rs = s2.executeQuery();
            while (rs.next()) {
                String poolID = rs.getString("pool_id");
                String entryID = rs.getString("entry_id");
                String fullID = poolID + "." + entryID;
                long time = rs.getLong("time_available");
                entryUnlockTimes.put(fullID, time);
            }
        } catch (SQLException e) {
            logSQLError("Failed to load player cooldown data for " + player.getName(), e);
            return null;
        } finally {
            if (rs != null) rs.close();
        }

        return new LoadedPlayer(player, new PlayerCooldown(poolUnlockTimes, entryUnlockTimes));
    }

    public void savePlayerData(LoadedPlayer loadedPlayer) {
        try (Connection connection = pool.getConnection();
             PreparedStatement s1 = connection.prepareStatement(SAVE_POOL_DATA);
             PreparedStatement s2 = connection.prepareStatement(SAVE_ENTRY_DATA)) {

            PlayerCooldown playerCooldown = loadedPlayer.getCooldowns();

            s1.setString(1, loadedPlayer.getUniqueId().toString());
            for (Map.Entry<String, Long> entry : playerCooldown.getPoolUnlockTimes().entrySet()) {
                s1.setString(2, entry.getKey());
                s1.setLong(3, entry.getValue());
                s1.addBatch();
            }
            s1.executeBatch();

            s2.setString(1, loadedPlayer.getUniqueId().toString());
            for (Map.Entry<String, Long> entry : playerCooldown.getEntryUnlockTimes().entrySet()) {
                // ID is of the form pool.entry -> split by a . character
                String[] arr = entry.getKey().split("\\.");
                s2.setString(2, arr[0]);
                s2.setString(3, arr[1]);
                s2.setLong(4, entry.getValue());
                s2.addBatch();
            }
            s2.executeBatch();

        } catch (SQLException e) {
            logSQLError("Failed to save player cooldown data for " + loadedPlayer.getName(), e);
        }
    }

    public void savePlayerData(Collection<LoadedPlayer> loadedPlayers) {
        try (Connection connection = pool.getConnection();
             PreparedStatement s1 = connection.prepareStatement(SAVE_POOL_DATA);
             PreparedStatement s2 = connection.prepareStatement(SAVE_ENTRY_DATA)) {

            for (LoadedPlayer loadedPlayer : loadedPlayers) {
                PlayerCooldown playerCooldown = loadedPlayer.getCooldowns();

                s1.setString(1, loadedPlayer.getUniqueId().toString());
                for (Map.Entry<String, Long> entry : playerCooldown.getPoolUnlockTimes().entrySet()) {
                    s1.setString(2, entry.getKey());
                    s1.setLong(3, entry.getValue());
                    s1.addBatch();
                }

                s2.setString(1, loadedPlayer.getUniqueId().toString());
                for (Map.Entry<String, Long> entry : playerCooldown.getEntryUnlockTimes().entrySet()) {
                    // ID is of the form pool.entry -> split by a . character
                    String[] arr = entry.getKey().split("\\.");
                    s2.setString(2, arr[0]);
                    s2.setString(3, arr[1]);
                    s2.setLong(4, entry.getValue());
                    s2.addBatch();
                }
            }
            s1.executeBatch();
            s2.executeBatch();

        } catch (SQLException e) {
            logSQLError("Failed to save player cooldown data for all players", e);
        }
    }

    public boolean deletePoolData(RewardPool rewardPool) {
        try (Connection connection = pool.getConnection();
             PreparedStatement s1 = connection.prepareStatement(DELETE_POOL_ENTRY);
             PreparedStatement s2 = connection.prepareStatement(DELETE_POOL_ENTRIES)) {

            s1.setString(1, rewardPool.getId());
            s1.execute();

            s2.setString(1, rewardPool.getId());
            s2.execute();

            return true;
        } catch (SQLException e) {
            logSQLError("Failed to delete global cooldown associated with pool=" + rewardPool.getId(), e);
        }
        return false;
    }

    /**
     * Purges the database of all data referencing non-loaded pools.
     * @param rewardManager The RewardManager instance
     * @return A list with two sub-lists. The first contains removed pool IDs, the second contains removed entry IDs
     */
    public List<Collection<String>> purgeUnusedData(RewardManager rewardManager) {
        Collection<RewardPool> pools = rewardManager.getPools();
        if (pools.isEmpty()) return List.of(Collections.emptyList(), Collections.emptyList());

        Set<String> activePoolIDs = pools.stream().map(RewardPool::getId).collect(Collectors.toSet());
        Set<String> activeEntryIDs = new HashSet<>();
        for (RewardPool rewardPool : pools) {
            activeEntryIDs.addAll(rewardPool.getEntryIDs());
        }

        try (Connection connection = pool.getConnection();
             ResultSet rs1 = connection.prepareStatement(GET_ALL_POOL_IDS_POOL).executeQuery();
             ResultSet rs2 = connection.prepareStatement(GET_ALL_POOL_IDS_ENTRY).executeQuery();
             PreparedStatement s1 = connection.prepareStatement(DELETE_POOL_ENTRY);
             PreparedStatement s2 = connection.prepareStatement(DELETE_ENTRY_ENTRY)) {

            List<String> databasePoolIDs = new ArrayList<>();
            List<String> databaseEntryIDs = new ArrayList<>();

            while (rs1.next()) {
                databasePoolIDs.add(rs1.getString("pool_id"));
            }
            while (rs2.next()) {
                databaseEntryIDs.add(rs2.getString("pool_id") + "." + rs2.getString("entry_id"));
            }

            if (!databasePoolIDs.isEmpty()) {
                databasePoolIDs = databasePoolIDs.stream().filter(id -> !activePoolIDs.contains(id)).toList();
                for (String poolID : databasePoolIDs) {
                    s1.setString(1, poolID);
                    s1.addBatch();
                }
                s1.executeBatch();
            }

            if (!databaseEntryIDs.isEmpty()) {
                databaseEntryIDs = databaseEntryIDs.stream().filter(id -> !activeEntryIDs.contains(id)).toList();
                for (String entryID : databaseEntryIDs) {
                    String[] arr = entryID.split("\\.");
                    s2.setString(1, arr[0]);
                    s2.setString(2, arr[1]);
                    s2.addBatch();
                }
                s2.executeBatch();
            }

            return List.of(databasePoolIDs, databaseEntryIDs);

        } catch (SQLException e) {
            logSQLError("Failed to purge unused pool global data", e);
            return List.of(Collections.emptyList(), Collections.emptyList());
        }
    }
}
