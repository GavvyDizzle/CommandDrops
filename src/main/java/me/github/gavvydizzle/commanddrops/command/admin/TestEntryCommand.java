package me.github.gavvydizzle.commanddrops.command.admin;

import com.github.mittenmc.serverutils.SubCommand;
import com.github.mittenmc.serverutils.command.WildcardCommand;
import me.github.gavvydizzle.commanddrops.command.AdminCommandManager;
import me.github.gavvydizzle.commanddrops.player.LoadedPlayer;
import me.github.gavvydizzle.commanddrops.player.PlayerManager;
import me.github.gavvydizzle.commanddrops.pool.RewardManager;
import me.github.gavvydizzle.commanddrops.pool.RewardPool;
import me.github.gavvydizzle.commanddrops.pool.entry.RewardEntry;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TestEntryCommand extends SubCommand implements WildcardCommand {

    private final PlayerManager playerManager;
    private final RewardManager rewardManager;

    public TestEntryCommand(AdminCommandManager adminCommandManager, PlayerManager playerManager, RewardManager rewardManager) {
        this.playerManager = playerManager;
        this.rewardManager = rewardManager;

        setName("testEntry");
        setDescription("Test pool entries");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " testEntry <poolID> <entryID>");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;

        if (args.length < 3) {
            sender.sendMessage(getColoredSyntax());
            return;
        }

        RewardPool pool = rewardManager.getPool(args[1]);
        if (pool == null) {
            sender.sendMessage(ChatColor.RED + "Invalid pool ID: " + args[1]);
            return;
        }

        RewardEntry entry = pool.getEntry(args[2]);
        if (entry == null) {
            sender.sendMessage(ChatColor.RED + "Invalid entry ID: " + args[2] + "(pool=" + pool.getId() + ")");
            return;
        }

        LoadedPlayer lp = playerManager.getPlayerData(player);
        if (lp == null) {
            sender.sendMessage(ChatColor.RED + "Failed to load your player data");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Giving rewards for pool=" + ChatColor.AQUA + pool.getId() +
                ChatColor.YELLOW + " entry=" + ChatColor.AQUA + entry.getId());
        entry.reward(lp);
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], rewardManager.getPoolIDs(), list);
        } else if (args.length == 3) {
            RewardPool pool = rewardManager.getPool(args[1]);
            if (pool != null) {
                StringUtil.copyPartialMatches(args[2], pool.getEntryIDs(), list);
            }
        }
        return list;
    }

    @Override
    public Collection<String> getWildcardValues(int index, String[] args) {
        if (index == 2) {
            RewardPool pool = rewardManager.getPool(args[1]);
            if (pool != null) {
                return pool.getEntryIDs();
            }
        }
        return Collections.emptyList();
    }
}
