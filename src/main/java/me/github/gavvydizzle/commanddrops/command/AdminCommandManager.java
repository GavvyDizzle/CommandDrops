package me.github.gavvydizzle.commanddrops.command;

import com.github.mittenmc.serverutils.CommandManager;
import com.github.mittenmc.serverutils.command.HelpCommand;
import me.github.gavvydizzle.commanddrops.CommandDrops;
import me.github.gavvydizzle.commanddrops.command.admin.*;
import me.github.gavvydizzle.commanddrops.gui.InventoryManager;
import me.github.gavvydizzle.commanddrops.player.PlayerManager;
import me.github.gavvydizzle.commanddrops.pool.RewardManager;
import org.bukkit.command.PluginCommand;

public class AdminCommandManager extends CommandManager {

    public AdminCommandManager(PluginCommand command, CommandDrops instance, PlayerManager playerManager, RewardManager rewardManager, InventoryManager inventoryManager) {
        super(command);

        registerCommand(new HelpCommand.HelpCommandBuilder(this).build());
        registerCommand(new OpenPoolListMenuCommand(this, inventoryManager));
        registerCommand(new PurgeDatabaseCommand(this, instance, playerManager, rewardManager));
        registerCommand(new ReloadCommand(this, instance));
        registerCommand(new SimulationCommand(this, playerManager, rewardManager));
        registerCommand(new SimulationIgnoreCooldownCommand(this, playerManager, rewardManager));
        registerCommand(new TestEntryCommand(this, playerManager, rewardManager));
    }
}
