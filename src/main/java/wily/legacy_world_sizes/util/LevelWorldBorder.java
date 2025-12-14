package wily.legacy_world_sizes.util;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;

public interface LevelWorldBorder {
    static WorldBorder withLevel(WorldBorder border, Level level) {
        if (border instanceof LevelWorldBorder levelWorldBorder) levelWorldBorder.setLevel(level);
        return border;
    }

    void setLevel(Level level);

    Level getLevel();
}
