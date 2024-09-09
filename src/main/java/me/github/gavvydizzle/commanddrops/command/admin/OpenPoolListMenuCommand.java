package me.github.gavvydizzle.commanddrops.command.admin;

import com.github.mittenmc.serverutils.SubCommand;
import me.github.gavvydizzle.commanddrops.command.AdminCommandManager;
import me.github.gavvydizzle.commanddrops.gui.InventoryManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class OpenPoolListMenuCommand extends SubCommand {

    private final InventoryManager inventoryManager;

    public OpenPoolListMenuCommand(AdminCommandManager adminCommandManager, InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;

        setName("list");
        setDescription("Opens the reward pool list menu");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " list");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            inventoryManager.openFileSystemMenu(player);
        }
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

}