package me.github.gavvydizzle.commanddrops.player;

import com.github.mittenmc.serverutils.player.profile.PlayerProfile;
import lombok.Getter;
import me.github.gavvydizzle.commanddrops.cooldown.PlayerCooldown;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class LoadedPlayer extends PlayerProfile {

    @Getter
    private final PlayerCooldown cooldowns;

    public LoadedPlayer(@NotNull Player player) {
        super(player);
        cooldowns = new PlayerCooldown(new HashMap<>(), new HashMap<>());
    }

    public LoadedPlayer(@NotNull Player player, PlayerCooldown playerCooldown) {
        super(player);
        this.cooldowns = playerCooldown;
    }

    public LoadedPlayer(@NotNull OfflinePlayer offlinePlayer, PlayerCooldown playerCooldown) {
        super(offlinePlayer);
        this.cooldowns = playerCooldown;
    }
}
