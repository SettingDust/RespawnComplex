package me.settingdust.respawncomplex.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.settingdust.respawncomplex.ComplexSpawnPointsKt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class MixinBlockItem {

    @Shadow protected abstract BlockState getPlacementState(BlockPlaceContext context);

    @Inject(method = "place", at = @At(value = "RETURN", ordinal = 5))
    public void place(
            BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> cir,
            @Local(index = 2) BlockPlaceContext newContext,
            @Local BlockPos blockPos,
            @Local Level level
//            @Local(index = 6, print = true) Player player,
//            @Local(ordinal = 1) BlockState blockState
    ) {
        if (newContext.getPlayer() instanceof ServerPlayer serverPlayer) {
            ComplexSpawnPointsKt.syncBlockPlace(
                    (BlockItem) (Object) this, blockPos, (ServerLevel) level, serverPlayer, getPlacementState(newContext));
        }
    }
}
