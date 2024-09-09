package me.github.gavvydizzle.commanddrops.command.admin;

import com.github.mittenmc.serverutils.SubCommand;
import me.github.gavvydizzle.commanddrops.CommandDrops;
import me.github.gavvydizzle.commanddrops.command.AdminCommandManager;
import me.github.gavvydizzle.commanddrops.data.GlobalCooldownDatabase;
import me.github.gavvydizzle.commanddrops.data.PlayerCooldownDatabase;
import me.github.gavvydizzle.commanddrops.player.PlayerManager;
import me.github.gavvydizzle.commanddrops.pool.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.*;

public class PurgeDatabaseCommand extends SubCommand {

    private final CommandDrops instance;
    private final PlayerManager playerManager;
    private final RewardManager rewardManager;

    public PurgeDatabaseCommand(AdminCommandManager adminCommandManager, CommandDrops instance, PlayerManager playerManager, RewardManager rewardManager) {
        this.instance = instance;
        this.playerManager = playerManager;
        this.rewardManager = rewardManager;

        setName("purgeDatabase");
        setDescription("Purge the database of unused pool and entry references");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " purgeDatabase");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (CommandDrops.areCooldownsDisabled()) {
            sender.sendMessage(ChatColor.YELLOW + "[CommandDrops] This command is disabled when cooldowns are disabled");
            return;
        }

        GlobalCooldownDatabase globalCooldownDatabase = instance.getGlobalCooldownDatabase();
        PlayerCooldownDatabase playerCooldownDatabase = instance.getPlayerCooldownDatabase();

        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            Set<String> deletedPoolIDs = new HashSet<>();
            Set<String> deletedEntryIDs = new HashSet<>();

            if (globalCooldownDatabase != null) {
                List<Collection<String>> ret = globalCooldownDatabase.purgeUnusedData(instance.getRewardManager());
                deletedPoolIDs.addAll(ret.get(0));
                deletedEntryIDs.addAll(ret.get(1));
            }
            if (playerCooldownDatabase != null) {
                List<Collection<String>> ret = playerCooldownDatabase.purgeUnusedData(instance.getRewardManager());
                deletedPoolIDs.addAll(ret.get(0));
                deletedEntryIDs.addAll(ret.get(1));
            }

            if (deletedPoolIDs.isEmpty() && deletedEntryIDs.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "[CommandDrops] All data is valid. Nothing needed to be purged!");
                return;
            }

            Bukkit.getScheduler().runTask(instance, () -> {
                if (!deletedPoolIDs.isEmpty()) {
                    rewardManager.getGlobalCooldown().removePools(deletedPoolIDs);
                    playerManager.getAllPlayerData().forEach(lp -> lp.getCooldowns().removePools(deletedPoolIDs));
                    sender.sendMessage(ChatColor.GREEN + "[CommandDrops] Purged " + deletedPoolIDs.size() + " unique pools");
                }
                if (!deletedEntryIDs.isEmpty()) {
                    rewardManager.getGlobalCooldown().removeEntries(deletedEntryIDs);
                    playerManager.getAllPlayerData().forEach(lp -> lp.getCooldowns().removeEntries(deletedEntryIDs));
                    sender.sendMessage(ChatColor.GREEN + "[CommandDrops] Purged " + deletedEntryIDs.size() + " unique entries");
                }
            });
        });
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
