package wily.legacy_world_sizes.mixin.base;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;
import java.nio.file.Path;

@Mixin(RegionFileStorage.class)
public interface RegionFileStorageAccessor {
    @Invoker("<init>")
    static RegionFileStorage lws$create(RegionStorageInfo info, Path path, boolean sync) {
        throw new AssertionError();
    }

    @Invoker("write")
    void lws$write(ChunkPos pos, CompoundTag tag) throws IOException;
}
