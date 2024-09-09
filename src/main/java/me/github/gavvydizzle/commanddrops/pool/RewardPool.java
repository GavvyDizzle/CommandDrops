package me.github.gavvydizzle.commanddrops.pool;

import com.github.mittenmc.serverutils.Numbers;
import com.github.mittenmc.serverutils.RandomValuePair;
import com.github.mittenmc.serverutils.ServerUtils;
import com.github.mittenmc.serverutils.file.FileEntity;
import com.github.mittenmc.serverutils.loottable.LootTable;
import lombok.Getter;
import me.github.gavvydizzle.commanddrops.CommandDrops;
import me.github.gavvydizzle.commanddrops.cooldown.GlobalCooldown;
import me.github.gavvydizzle.commanddrops.event.AttemptRewardRollEvent;
import me.github.gavvydizzle.commanddrops.player.LoadedPlayer;
import me.github.gavvydizzle.commanddrops.pool.entry.RewardEntry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RewardPool extends FileEntity {

    private static final Pattern entryKeyPattern = Pattern.compile("[\\w-]{1,16}");

    @Getter private final String id;
    private final List<ActivationType> activationTypes;
    @Getter private double rollPercentChance;
    private final List<String> permissions;
    private final GlobalCooldown globalCooldowns;
    private RandomValuePair playerCooldownRange, globalCooldownRange;

    private LootTable<RewardEntry, RewardEntry> lootTable;
    private final Map<String, RewardEntry> entries;

    public RewardPool(File file, GlobalCooldown globalCooldowns) {
        super(file);
        this.globalCooldowns = globalCooldowns;

        this.id = super.getFileNameLowerCase().replace(".yml", "");
        this.activationTypes = new ArrayList<>();
        this.permissions = new ArrayList<>();
        this.lootTable = new LootTable<>();
        this.entries = new HashMap<>();

        super.reload();
    }

    @Override
    public void reloadData(FileConfiguration config) {
        activationTypes.clear();
        permissions.clear();

        config.addDefault("types", List.of());
        config.addDefault("activation_chance", 0.01);
        config.addDefault("permissions", List.of());
        config.addDefault("cooldown.player.min", 0);
        config.addDefault("cooldown.player.max", 0);
        config.addDefault("cooldown.global.min", 0);
        config.addDefault("cooldown.global.max", 0);

        if (!config.isConfigurationSection("entries")) config.createSection("entries");
        ConfigurationSection entriesSection = config.getConfigurationSection("entries");
        assert entriesSection != null;

        for (String type : config.getStringList("types")) {
            ActivationType activationType = ActivationType.get(type);
            if (activationType == null) {
                ServerUtils.getInstance().getLogger().warning("Invalid activation type '" + type + "' in " + super.getFileName());
            } else if (activationType == ActivationType.COMMAND) {
                ServerUtils.getInstance().getLogger().warning("Invalid activation type '" + type + "' in " + super.getFileName() + ". This type is for internal use only!");
            } else {
                activationTypes.add(activationType);
            }
        }

        if (activationTypes.isEmpty()) {
            ServerUtils.getInstance().getLogger().warning("Pool defined in " + super.getFileName() + " has no activation types defined. It cannot activate!");
        }

        rollPercentChance = Numbers.constrain(config.getDouble("activation_chance"), 0, 1) * 100;
        permissions.addAll(config.getStringList("permissions"));

        this.playerCooldownRange = new RandomValuePair(
                Math.max(0, Numbers.parseSeconds(config.getString("cooldown.player.min"))),
                Math.max(0, Numbers.parseSeconds(config.getString("cooldown.player.max")))
        );
        this.globalCooldownRange = new RandomValuePair(
                Math.max(0, Numbers.parseSeconds(config.getString("cooldown.global.min"))),
                Math.max(0, Numbers.parseSeconds(config.getString("cooldown.global.max")))
        );

        for (String key : entriesSection.getKeys(false)) {
            if (!entriesSection.isConfigurationSection(key)) entriesSection.createSection(key);
            ConfigurationSection section = entriesSection.getConfigurationSection(key);
            assert section != null;

            Matcher matcher = entryKeyPattern.matcher(key);
            if (!matcher.matches()) {
                CommandDrops.getInstance().getLogger().warning("The pool entry in " + getFileName() + " has an invalid key '" + key + "' . Ensure it is 16 characters or less (regex: " + entryKeyPattern.pattern() + ")");
                continue;
            }

            RewardEntry entry = new RewardEntry(key, section, this);
            lootTable.add(entry);
            entries.put(entry.getId(), entry);
        }
    }

    public boolean canAttempt(ActivationType type, LoadedPlayer lp) {
        return hasActivationType(type) && !onCooldown(lp) && lp.hasPermission(permissions);
    }

    private boolean hasActivationType(ActivationType type) {
        return activationTypes.contains(type);
    }

    private boolean onCooldown(LoadedPlayer player) {
        if (CommandDrops.areCooldownsDisabled()) return false;
        return globalCooldowns.onCooldown(this) || player.getCooldowns().onCooldown(this);
    }

    /**
     * Attempts to grant rewards for the player.
     * @param e The original event this was triggered from
     * @param lp The player
     * @param activationType What action initiated this attempt
     */
    public void attempt(@Nullable Event e, LoadedPlayer lp, ActivationType activationType) {
        attempt(e, lp, activationType, 1);
    }

    /**
     * Attempts to grant rewards for the player.
     * @param e The original event this was triggered from
     * @param lp The player
     * @param activationType What action initiated this attempt
     * @param numAttempts The number of attempts
     */
    public void attempt(@Nullable Event e, LoadedPlayer lp, ActivationType activationType, int numAttempts) {
        int successes = 0;
        for (int i = 0; i < numAttempts; i++) {
            if (shouldRoll(e, lp, activationType)) successes++;
        }

        if (successes > 0) roll(lp, successes);
    }

    /**
     * Attempts to grant rewards for the player.
     * @param e The original event this was triggered from
     * @param lp The player
     * @param activationType What action initiated this attempt
     * @param numAttempts The number of attempts
     */
    public void attemptIgnoreCooldowns(@Nullable Event e, LoadedPlayer lp, ActivationType activationType, int numAttempts) {
        int successes = 0;
        for (int i = 0; i < numAttempts; i++) {
            if (shouldRoll(e, lp, activationType)) successes++;
        }

        if (successes > 0) rollIgnoreCooldowns(lp, successes);
    }

    /**
     * Attempts to grant rewards for the player.
     * This will not fire any events.
     * @param lp The player
     */
    public void attemptWithoutEvent(LoadedPlayer lp) {
        attemptWithoutEvent(lp, 1);
    }

    /**
     * Attempts to grant rewards for the player.
     * This will not fire any events.
     * @param lp The player
     * @param numAttempts The number of attempts
     */
    public void attemptWithoutEvent(LoadedPlayer lp, int numAttempts) {
        int successes = 0;
        for (int i = 0; i < numAttempts; i++) {
            if (shouldRollWithoutEvent()) successes++;
        }

        if (successes > 0) roll(lp, successes);
    }

    /**
     * Attempts to grant rewards for the player.
     * This will not fire any events.
     * @param lp The player
     * @param numAttempts The number of attempts
     */
    public void attemptWithoutEventIgnoreCooldowns(LoadedPlayer lp, int numAttempts) {
        int successes = 0;
        for (int i = 0; i < numAttempts; i++) {
            if (shouldRollWithoutEvent()) successes++;
        }

        if (successes > 0) rollIgnoreCooldowns(lp, successes);
    }

    /**
     * Randomly determines if a roll should occur.
     * An {@link AttemptRewardRollEvent} will be fired so other plugins can modify the chance.
     * @param e The original event this was triggered from
     * @param lp The player
     * @param activationType The activation type
     * @return If this attempt should result in a roll
     */
    private boolean shouldRoll(@Nullable Event e, LoadedPlayer lp, ActivationType activationType) {
        if (rollPercentChance <= 0) return false;

        AttemptRewardRollEvent attemptRewardRollEvent = new AttemptRewardRollEvent(e, lp.getPlayer(), activationType, this, rollPercentChance);
        Bukkit.getPluginManager().callEvent(attemptRewardRollEvent);
        if (attemptRewardRollEvent.isCancelled()) return false;

        return Numbers.percentChance(attemptRewardRollEvent.getRollPercentChance());
    }

    /**
     * Randomly determines if a roll should occur WITHOUT firing any event.
     * @return If this attempt should result in a roll
     */
    private boolean shouldRollWithoutEvent() {
        if (rollPercentChance <= 0) return false;
        return Numbers.percentChance(rollPercentChance);
    }

    /**
     * Give one loot table reward to this player.
     * If the player does not have permission or this entry is on cooldown for them,
     * then the reward will silently not be given.
     * @param lp The player
     */
    public void roll(LoadedPlayer lp) {
        roll(lp, 1);
    }

    /**
     * Give n loot table rewards to this player.
     * If the player does not have permission or this entry is on cooldown for them,
     * then the reward will silently not be given.
     * @param lp The player
     * @param n The number of loot table rolls to perform with replacement
     */
    public void roll(LoadedPlayer lp, int n) {
        List<RewardEntry> entry = lootTable.getDropsWithReplacement(n);
        boolean hit = false;

        for (RewardEntry e : entry) {
            if (!e.onCooldown(globalCooldowns, lp) && e.hasPermission(lp.getPlayer())) {
                hit = true;
                e.reward(lp);
                globalCooldowns.placeOnCooldown(e);
                lp.getCooldowns().placeOnCooldown(e);
            }
        }

        if (hit) {
            globalCooldowns.placeOnCooldown(this);
            lp.getCooldowns().placeOnCooldown(this);
        }
    }

    /**
     * Give n loot table rewards to this player.
     * If the player does not have permission or this entry is on cooldown for them,
     * then the reward will silently not be given.
     * @param lp The player
     * @param n The number of loot table rolls to perform with replacement
     */
    public void rollIgnoreCooldowns(LoadedPlayer lp, int n) {
        List<RewardEntry> entry = lootTable.getDropsWithReplacement(n);

        for (RewardEntry e : entry) {
            if (e.hasPermission(lp.getPlayer())) {
                e.reward(lp);
            }
        }
    }

    /**
     * Handles when the weight of an entry changes.
     * This will invalidate all entry items, so they will regenerate the next time they are needed.
     * The update will also be saved to the config.
     */
    public void handleWeightChange(RewardEntry entry) {
        lootTable = new LootTable<>(entries.values());
        entries.values().forEach(RewardEntry::setItemNull);

        getConfig().set("entries." + entry.getId() + ".weight", entry.getWeight());
        saveConfigAsync();
    }

    public int getRandomPlayerCooldown() {
        return playerCooldownRange.getRandomInt();
    }

    public int getRandomGlobalCooldown() {
        return globalCooldownRange.getRandomInt();
    }

    public List<String> generateMenuLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Activation Chance: &e" + rollPercentChance/100 + " &8&o" + rollPercentChance + "%");
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

        return lore;
    }

    @Nullable
    public RewardEntry getEntry(String id) {
        return entries.get(id);
    }

    public Collection<RewardEntry> getEntries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public Collection<String> getEntryIDs() {
        return entries.keySet();
    }

    public Collection<ActivationType> getActivationTypes() {
        return Collections.unmodifiableList(activationTypes);
    }

    public double getTotalWeight() {
        return lootTable.getTotalWeight();
    }
}
