package me.github.gavvydizzle.commanddrops.event;

import com.github.mittenmc.serverutils.Numbers;
import lombok.Getter;
import me.github.gavvydizzle.commanddrops.pool.ActivationType;
import me.github.gavvydizzle.commanddrops.pool.RewardPool;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AttemptRewardRollEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean isCancelled;

    @Getter @Nullable
    private final Event parentEvent;
    @Getter private final ActivationType activationType;
    @Getter private final RewardPool rewardPool;
    @Getter private double rollPercentChance;

    public AttemptRewardRollEvent(@Nullable Event parentEvent, Player player, ActivationType activationType, RewardPool pool, double rollPercentChance) {
        super(player);
        this.isCancelled = false;

        this.parentEvent = parentEvent;
        this.activationType = activationType;
        this.rewardPool = pool;
        this.rollPercentChance = rollPercentChance;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    /**
     * Multiplies the current chance by the given multiplier
     * @param multiplier The multiplier
     */
    public void multiplyRollChance(double multiplier) {
        rollPercentChance *= multiplier;
    }

    /**
     * Sets the percent chance for this pool to roll.
     * Amounts >= 100% will always cause the roll to occur.
     * @param chance The new percent chance for this pool to roll
     */
    public void setRollPercentChance(double chance) {
        rollPercentChance = Numbers.constrain(chance, 0, 100);
    }
}