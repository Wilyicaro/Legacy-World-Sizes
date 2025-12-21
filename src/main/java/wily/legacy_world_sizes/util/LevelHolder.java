package wily.legacy_world_sizes.util;

import net.minecraft.world.level.Level;

public interface LevelHolder {
    static <T> T withLevel(T holder, Level level) {
        if (holder instanceof LevelHolder levelWorldBorder) levelWorldBorder.setLevel(level);
        return holder;
    }

    void setLevel(Level level);

    Level getLevel();
}
