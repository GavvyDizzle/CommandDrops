package me.github.gavvydizzle.commanddrops.gui;

import com.github.mittenmc.serverutils.gui.ClickableMenu;
import com.github.mittenmc.serverutils.gui.MenuManager;
import com.github.mittenmc.serverutils.gui.filesystem.FileSystemMenu;
import me.github.gavvydizzle.commanddrops.gui.filesystem.PoolFileSystemMenu;
import me.github.gavvydizzle.commanddrops.pool.RewardManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryManager extends MenuManager {

    private final RewardManager rewardManager;

    public InventoryManager(JavaPlugin instance, RewardManager rewardManager) {
        super(instance);
        this.rewardManager = rewardManager;
    }

    public void openFileSystemMenu(Player player) {
        PoolFileSystemMenu menu = new PoolFileSystemMenu(this, player, "Reward Pool Database", rewardManager.getRootNode());
        openMenu(player, menu);
    }

    public void refreshFileSystemMenus() {
        for (ClickableMenu menu : getViewers().values()) {
            if (menu instanceof FileSystemMenu fileSystemMenu) {
                fileSystemMenu.refresh(rewardManager.getRootNode());
            }
        }
    }
}
