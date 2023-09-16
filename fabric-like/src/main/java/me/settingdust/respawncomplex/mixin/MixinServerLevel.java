package me.settingdust.respawncomplex.mixin;

import me.settingdust.respawncomplex.ComplexSpawnPointsKt;
import me.settingdust.respawncomplex.Config;
import me.settingdust.respawncomplex.PlayerBreakBlocksEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class MixinServerLevel {
//    @Inject(method = "onBlockStateChange", at = @At("HEAD"))
//    private void respawncomplex$addContextToPlayerBreakBlocksEvent(BlockPos pos, BlockState blockState, BlockState newState, CallbackInfo ci) {
//        var context = PlayerBreakBlocksEvent.Companion.getContext();
//        if (context == null) return;
//        if (context.getLevel() != (Object) this) return;
//        if (!newState.is(Blocks.AIR)) return;
//        context.getBlockPoses().add(pos);
//    }

    @Inject(method = "onBlockStateChange", at = @At("HEAD"))
    private void respawncomplex$syncBlockBreak(BlockPos pos, BlockState blockState, BlockState newState, CallbackInfo ci) {
        ComplexSpawnPointsKt.syncBlockBreak((ServerLevel) (Object) this, pos, blockState, newState);
    }

    @Inject(method = "onBlockStateChange", at = @At("HEAD"))
    private void respawncomplex$syncBlockPlace(BlockPos pos, BlockState blockState, BlockState newState, CallbackInfo ci) {
        ComplexSpawnPointsKt.syncBlockPlace((ServerLevel) (Object) this, pos, blockState, newState);
    }
}
