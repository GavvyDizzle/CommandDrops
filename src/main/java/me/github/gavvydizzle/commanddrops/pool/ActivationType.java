package me.github.gavvydizzle.commanddrops.pool;

import org.jetbrains.annotations.Nullable;

public enum ActivationType {
    COMMAND(null),
    FARMING,
    FISHING,
    MINING;

    @Nullable
    public final String regionFlagName;

    ActivationType() {
        this.regionFlagName = "cmddrops-" + this.name().toLowerCase();
    }

    ActivationType(@Nullable String regionFlagName) {
        this.regionFlagName = regionFlagName;
    }

    @Nullable
    public static ActivationType get(String str) {
        for (ActivationType type : values()) {
            if (type.name().equalsIgnoreCase(str)) return type;
        }
        return null;
    }
}
