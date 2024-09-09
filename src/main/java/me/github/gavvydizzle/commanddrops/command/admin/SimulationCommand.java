package me.github.gavvydizzle.commanddrops.command.admin;

import com.github.mittenmc.serverutils.Numbers;
import com.github.mittenmc.serverutils.SubCommand;
import com.github.mittenmc.serverutils.command.WildcardCommand;
import me.github.gavvydizzle.commanddrops.command.AdminCommandManager;
import me.github.gavvydizzle.commanddrops.player.LoadedPlayer;
import me.github.gavvydizzle.commanddrops.player.PlayerManager;
import me.github.gavvydizzle.commanddrops.pool.ActivationType;
import me.github.gavvydizzle.commanddrops.pool.RewardManager;
import me.github.gavvydizzle.commanddrops.pool.RewardPool;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SimulationCommand extends SubCommand implements WildcardCommand {

    private static final int MAX_ATTEMPT_SIMULATIONS = 10000;
    private static final int MAX_ROLL_SIMULATIONS = 1000;

    private final PlayerManager playerManager;
    private final RewardManager rewardManager;
    private final List<String> typeArgs = List.of("attempt", "attemptWithEvent", "roll");
    private final List<String> amountArgs = List.of("1");

    public SimulationCommand(AdminCommandManager adminCommandManager, PlayerManager playerManager, RewardManager rewardManager) {
        this.playerManager = playerManager;
        this.rewardManager = rewardManager;

        setName("simulate");
        setDescription("Simulate reward events");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " simulate <type> <poolID> <player> <amount>");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(getColoredSyntax());
            return;
        }

        if (!typeArgs.contains(args[1])) {
            sender.sendMessage(ChatColor.RED + "Invalid simulation type: " + args[1]);
            return;
        }

        RewardPool pool = rewardManager.getPool(args[2]);
        if (pool == null) {
            sender.sendMessage(ChatColor.RED + "Invalid pool ID: " + args[2]);
            return;
        }

        Player player = Bukkit.getPlayer(args[3]);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Invalid player: " + args[3]);
            return;
        }

        LoadedPlayer lp = playerManager.getPlayerData(player);
        if (lp == null) {
            sender.sendMessage(ChatColor.RED + "Failed to load player data for " + player.getName());
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[4]);
        } catch (Exception e) {
            amount = -1;
        }
        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[4]);
            return;
        }

        if (args[1].equalsIgnoreCase("attempt")) {
            amount = Numbers.constrain(amount, 1, MAX_ATTEMPT_SIMULATIONS);
            pool.attemptWithoutEvent(lp, amount);
            if (sender instanceof Player) sender.sendMessage(ChatColor.GREEN + "Sent " + amount + " " + ChatColor.YELLOW + pool.getId() +
                    ChatColor.GREEN + " reward pool attempts (without events) to " + player.getName());
        } else if (args[1].equalsIgnoreCase("attemptwithevent")) {
            amount = Numbers.constrain(amount, 1, MAX_ATTEMPT_SIMULATIONS);
            pool.attempt(null, lp, ActivationType.COMMAND, amount);
            if (sender instanceof Player) sender.sendMessage(ChatColor.GREEN + "Sent " + amount + " " + ChatColor.YELLOW + pool.getId() +
                    ChatColor.GREEN + " reward pool attempts (with events) to " + player.getName());
        } else {
            amount = Numbers.constrain(amount, 1, MAX_ROLL_SIMULATIONS);
            pool.roll(lp, amount);
            if (sender instanceof Player) sender.sendMessage(ChatColor.GREEN + "Sent " + amount + " " + ChatColor.YELLOW + pool.getId() +
                    ChatColor.GREEN + " reward pool rolls to " + player.getName());
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], typeArgs, list);
        } else if (args.length == 3) {
            StringUtil.copyPartialMatches(args[2], rewardManager.getPoolIDs(), list);
        } else if (args.length == 4) {
            return null;
        } else if (args.length == 5) {
            StringUtil.copyPartialMatches(args[4], amountArgs, list);
        }
        return list;
    }

    @Override
    public Collection<String> getWildcardValues(int index, String[] args) {
        if (index == 2) {
            return rewardManager.getPoolIDs();
        }
        return Collections.emptyList();
    }
}