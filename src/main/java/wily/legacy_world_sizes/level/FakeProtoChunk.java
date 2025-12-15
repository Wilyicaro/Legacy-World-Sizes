package wily.legacy_world_sizes.level;

import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;

public class FakeProtoChunk extends ImposterProtoChunk {
    public FakeProtoChunk(LevelChunk levelChunk) {
        super(levelChunk, true);
    }
}
