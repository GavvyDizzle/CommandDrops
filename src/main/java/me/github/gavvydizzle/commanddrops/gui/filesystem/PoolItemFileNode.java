package me.github.gavvydizzle.commanddrops.gui.filesystem;

import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.gui.filesystem.tree.ItemFileNode;
import com.github.mittenmc.serverutils.gui.filesystem.tree.Node;
import me.github.gavvydizzle.commanddrops.pool.RewardPool;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PoolItemFileNode extends ItemFileNode {

    private final RewardPool pool;

    public PoolItemFileNode(@Nullable Node parent, RewardPool pool, boolean isFolder) {
        super(parent, pool.getFileName(), isFolder);
        this.pool = pool;
    }

    public PoolItemFileNode(@Nullable Node parent, RewardPool pool, boolean isFolder, Material folderMaterial, Material fileMaterial) {
        super(parent, pool.getFileName(), isFolder, folderMaterial, fileMaterial);
        this.pool = pool;
    }

    @Override
    public ItemStack generateDisplayItem() {
        ItemStack itemStack = new ItemStack(isFolder() ? getFolderMaterial() : getFileMaterial());
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;

        meta.setDisplayName(Colors.conv("&f" + pool.getId()));

        List<String> lore = new ArrayList<>();
        lore.add("&8" + getName());

        if (isLeaf()) {
            lore.add("&eEmpty");
        } else {
            lore.add("&e" + getChildren().size() + " Entries");
        }
        lore.add("");
        lore.addAll(pool.generateMenuLore());
        meta.setLore(Colors.conv(lore));

        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
