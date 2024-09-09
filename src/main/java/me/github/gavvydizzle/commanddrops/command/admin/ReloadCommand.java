package me.github.gavvydizzle.commanddrops.command.admin;

import com.github.mittenmc.serverutils.SubCommand;
import me.github.gavvydizzle.commanddrops.CommandDrops;
import me.github.gavvydizzle.commanddrops.command.AdminCommandManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class ReloadCommand extends SubCommand {

    private final CommandDrops instance;

    public ReloadCommand(AdminCommandManager adminCommandManager, CommandDrops instance) {
        this.instance = instance;

        setName("reload");
        setDescription("Reload this plugin");
        setSyntax("/" + adminCommandManager.getCommandDisplayName() + " reload");
        setColoredSyntax(ChatColor.YELLOW + getSyntax());
        setPermission(adminCommandManager.getPermissionPrefix() + getName().toLowerCase());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        try {
            instance.reloadConfig();
            instance.reload();

            instance.getRewardManager().reload();
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Reload error encountered. Please check the console for errors");
            instance.getLogger().log(Level.SEVERE, "Plugin reload failed", e);
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "[CommandDrops] Reload Successful");
    }

    @Override
    public List<String> getSubcommandArguments(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
