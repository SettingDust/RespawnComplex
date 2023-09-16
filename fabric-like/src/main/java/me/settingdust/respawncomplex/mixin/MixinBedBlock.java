package me.settingdust.respawncomplex.mixin;

import me.settingdust.respawncomplex.ComplexSpawnable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BedBlock.class)
public class MixinBedBlock implements ComplexSpawnable {
    @Shadow
    public static boolean canSetSpawn(Level level) {
        return false;
    }

    @Override
    public boolean respawnComplex$isValid(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return canSetSpawn(level);
    }

    @Override
    public void respawnComplex$onRespawn(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
    }
}
