package me.settingdust.respawncomplex.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
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
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class MixinBlockItem {

    @Shadow
    protected abstract BlockState getPlacementState(BlockPlaceContext context);

    @ModifyReceiver(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;gameEvent(Lnet/minecraft/world/level/gameevent/GameEvent;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V"))
    public Level place(
            Level level,
            GameEvent event,
            BlockPos blockPos,
            GameEvent.Context context
    ) {
        if (context.sourceEntity() instanceof ServerPlayer serverPlayer) {
            ComplexSpawnPointsKt.syncBlockPlace(
                    (ServerLevel) level, blockPos, serverPlayer, context.affectedState());
        }
        return level;
    }
}
