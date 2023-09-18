package me.settingdust.respawncomplex.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.settingdust.respawncomplex.ComplexSpawnable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RespawnAnchorBlock.class)
public class MixinRespawnAnchorBlock implements ComplexSpawnable {
    @Shadow
    public static boolean canSetSpawn(Level level) {
        return false;
    }

    @Override
    public boolean respawnComplex$isValid(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return canSetSpawn(level) && state.getValue(RespawnAnchorBlock.CHARGE) > 0;
    }

    @Override
    public void respawnComplex$onRespawn(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
        level.setBlock(
                pos, state.setValue(RespawnAnchorBlock.CHARGE, state.getValue(RespawnAnchorBlock.CHARGE) - 1), 1 | 2);
    }

    @Inject(
            method = "use",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/RespawnAnchorBlock;canSetSpawn(Lnet/minecraft/world/level/Level;)Z",
                            shift = At.Shift.AFTER),
            cancellable = true)
    private void respawnComplex$disableSpawn(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir) {
        cir.setReturnValue(InteractionResult.CONSUME);
        cir.cancel();
    }

    @Override
    public void respawnComplex$onActivate(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
        level.playSound(
                null,
                (double) pos.getX() + 0.5,
                (double) pos.getY() + 0.5,
                (double) pos.getZ() + 0.5,
                SoundEvents.RESPAWN_ANCHOR_SET_SPAWN,
                SoundSource.BLOCKS,
                1.0f,
                1.0f);
    }
}
