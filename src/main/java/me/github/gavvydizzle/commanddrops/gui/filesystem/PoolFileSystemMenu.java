package me.github.gavvydizzle.commanddrops.gui.filesystem;

import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.Numbers;
import com.github.mittenmc.serverutils.gui.filesystem.FileSystemMenu;
import com.github.mittenmc.serverutils.gui.filesystem.tree.ItemNode;
import com.github.mittenmc.serverutils.gui.filesystem.tree.Node;
import com.github.mittenmc.serverutils.gui.pages.DisplayItem;
import me.github.gavvydizzle.commanddrops.CommandDrops;
import me.github.gavvydizzle.commanddrops.gui.InventoryManager;
import me.github.gavvydizzle.commanddrops.pool.entry.RewardEntry;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PoolFileSystemMenu extends FileSystemMenu {

    private final static ItemStack helpItem;

    static {
        helpItem = new ItemStack(Material.BOOK);
        ItemMeta meta = helpItem.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Colors.conv("&fEntry Editing Help"));
        meta.setLore(Colors.conv(List.of(
                "&7- &eClick to edit weight"
        )));
        helpItem.setItemMeta(meta);
    }

    private final InventoryManager inventoryManager;
    private final Player player;

    private final AnvilGUI.Builder weightSelector;
    private RewardEntry selectedEntry;

    public PoolFileSystemMenu(InventoryManager inventoryManager, Player player, String inventoryName, Node root) {
        super(inventoryName, root);
        this.inventoryManager = inventoryManager;
        this.player = player;

        addClickableItem(45, new DisplayItem<>(helpItem));

        weightSelector = new AnvilGUI.Builder()
                .plugin(CommandDrops.getInstance())
                .title("Update Weight")
                .text("0")
                .onClose(stateSnapshot -> reopenDelayed())
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    double newWeight;
                    try {
                        newWeight = Double.parseDouble(stateSnapshot.getText());
                    } catch (Exception ignored) {
                        return Collections.emptyList();
                    }
                    newWeight = Math.max(newWeight, 0);

                    if (Math.abs(newWeight - selectedEntry.getWeight()) < 1e-3) return Collections.emptyList();

                    double oldWeight = selectedEntry.getWeight();
                    selectedEntry.pushWeightUpdate(newWeight);

                    player.sendMessage(ChatColor.GREEN + "Updated weight to " + Numbers.round(newWeight, 3) + " for " + selectedEntry.getId() + " (was " + oldWeight + ")");
                    return List.of(AnvilGUI.ResponseAction.close());
                });
    }

    public void reopenDelayed() {
        Bukkit.getScheduler().runTask(CommandDrops.getInstance(), () -> {
            inventoryManager.openMenu(player, this);
            super.update(super.getCurrentNode());
            selectedEntry = null;
        });
    }

    @Override
    public void onItemClick(InventoryClickEvent inventoryClickEvent, Player player, @NotNull Node node) {
        if (node.isLeaf() && node instanceof ItemNode<?> itemNode && itemNode.getData() instanceof RewardEntry entry) {
            selectedEntry = entry;
            weightSelector.itemRight(entry.getMenuItem(player)).open(player);
            return;
        }

        super.onItemClick(inventoryClickEvent, player, node);
    }
}
