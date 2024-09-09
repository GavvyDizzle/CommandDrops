package me.github.gavvydizzle.commanddrops.pool.entry;

import com.github.mittenmc.serverutils.Colors;
import com.github.mittenmc.serverutils.ConfigUtils;
import com.github.mittenmc.serverutils.Numbers;
import com.github.mittenmc.serverutils.RandomValuePair;
import com.github.mittenmc.serverutils.gui.pages.ItemGenerator;
import com.github.mittenmc.serverutils.loottable.LootTableEntry;
import lombok.Getter;
import me.github.gavvydizzle.commanddrops.CommandDrops;
import me.github.gavvydizzle.commanddrops.cooldown.GlobalCooldown;
import me.github.gavvydizzle.commanddrops.player.LoadedPlayer;
import me.github.gavvydizzle.commanddrops.pool.RewardPool;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RewardEntry implements Comparable<RewardEntry>, ItemGenerator, LootTableEntry<RewardEntry> {

    private static final Comparator<RewardEntry> comparator = Comparator.comparingDouble(RewardEntry::getWeight).reversed().thenComparing(RewardEntry::getId);

    private final RewardPool pool;
    @Getter private final String id;
    private double weight;
    private final List<String> commands, permissions;
    private final Material material;
    private final RandomValuePair playerCooldownRange, globalCooldownRange;

    private ItemStack itemStack;

    public RewardEntry(String id, ConfigurationSection section, RewardPool rewardPool) {
        // The entry requires the pool to be contained in its ID, so it is unique for hashing!
        this.id = rewardPool.getId() + "." + id;
        this.pool = rewardPool;

        section.addDefault("weight", 0);
        section.addDefault("menu_material", Material.NETHER_STAR.name());
        section.addDefault("commands", List.of());
        section.addDefault("permissions", List.of());
        section.addDefault("cooldown.player.min", 0);
        section.addDefault("cooldown.player.max", 0);
        section.addDefault("cooldown.global.min", 0);
        section.addDefault("cooldown.global.max", 0);

        this.weight = Math.max(section.getDouble("weight"), 0);
        this.material = ConfigUtils.getMaterial(section.getString("menu_material"), Material.NETHER_STAR);
        this.commands = section.getStringList("commands");
        this.permissions = section.getStringList("permissions");

        this.playerCooldownRange = new RandomValuePair(
                Math.max(0, Numbers.parseSeconds(section.getString("cooldown.player.min"))),
                Math.max(0, Numbers.parseSeconds(section.getString("cooldown.player.max")))
        );
        this.globalCooldownRange = new RandomValuePair(
                Math.max(0, Numbers.parseSeconds(section.getString("cooldown.global.min"))),
                Math.max(0, Numbers.parseSeconds(section.getString("cooldown.global.max")))
        );
    }

    public void pushWeightUpdate(double newWeight) {
        weight = newWeight;
        pool.handleWeightChange(this);
    }

    public boolean hasPermission(Player player) {
        for (String permission : permissions) {
            if (!player.hasPermission(permission)) return false;
        }
        return true;
    }

    public boolean onCooldown(GlobalCooldown globalCooldown, LoadedPlayer lp) {
        if (CommandDrops.areCooldownsDisabled()) return false;
        return globalCooldown.onCooldown(this) || lp.getCooldowns().onCooldown(this);
    }

    public void reward(LoadedPlayer lp) {
        Player player = lp.getPlayer();
        assert player != null;

        for (String cmd : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player_name}", player.getName()));
        }
    }

    @Override
    public RewardEntry map() {
        return this;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    private double getPercentRollChance() {
        if (pool.getTotalWeight() == 0) return 0;
        return Numbers.round(weight * 100 / pool.getTotalWeight(), 2);
    }

    private String getOneInXRollChance() {
        if (weight == 0) return "N/A";
        double val = Numbers.round(1 / (weight / pool.getTotalWeight()), 2);
        return "1/" + val;
    }

    private double getPercentPerActivation() {
        if (pool.getTotalWeight() == 0) return 0;
        return Numbers.round(weight * pool.getRollPercentChance() / pool.getTotalWeight(), 2);
    }

    private String getOneInXPerActivationChance() {
        if (weight == 0) return "N/A";
        double val = Numbers.round(1 / (weight * 0.01 * pool.getRollPercentChance() / pool.getTotalWeight()), 2);
        return "1/" + val;
    }

    private void generateItemStack() {
        itemStack = new ItemStack(weight <= 0 ? Material.BARRIER : material);
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Colors.conv("&f" + id));

        List<String> lore = new ArrayList<>();
        lore.add("&aWeight: &e" + Numbers.round(weight,4) + "/" + Numbers.round(pool.getTotalWeight(), 4));
        lore.add("&7- Roll Chance: &e" + getPercentRollChance() + "% &8&o(" + getOneInXRollChance() + ")");
        lore.add("&7- Overall Chance: &e" + getPercentPerActivation() + "% &8&o(" + getOneInXPerActivationChance() + ")");
        lore.add("");
        if (CommandDrops.areCooldownsEnabled()) {
            lore.add("&bCooldowns:");
            lore.add("&7- Player: &e" + Numbers.getTimeFormatted(playerCooldownRange.min()) + " to " + Numbers.getTimeFormatted(playerCooldownRange.max()));
            lore.add("&7- Global: &e" + Numbers.getTimeFormatted(globalCooldownRange.min()) + " to " + Numbers.getTimeFormatted(globalCooldownRange.max()));
            lore.add("");
        }
        lore.add("&aPermissions (" + permissions.size() + "):");
        for (String perm : permissions) {
            lore.add("&7- &e" + perm);
        }
        lore.add("");
        lore.add("&aCommands (" + commands.size() + "):");
        for (String cmd : commands) {
            lore.add("&7- &e" + cmd);
        }
        meta.setLore(Colors.conv(lore));

        itemStack.setItemMeta(meta);
    }

    @Override
    public @NotNull ItemStack getMenuItem(Player player) {
        if (itemStack == null) generateItemStack();
        return itemStack;
    }

    @Override
    public @Nullable ItemStack getPlayerItem(Player player) {
        return null;
    }

    @Override
    public int compareTo(@NotNull RewardEntry o) {
        return comparator.compare(this, o);
    }

    public int getRandomPlayerCooldown() {
        return playerCooldownRange.getRandomInt();
    }

    public int getRandomGlobalCooldown() {
        return globalCooldownRange.getRandomInt();
    }

    public void setItemNull() {
        itemStack = null;
    }
}
