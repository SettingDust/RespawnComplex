package me.settingdust.respawncomplex.mixin;

import me.settingdust.respawncomplex.ComplexSpawnPointsKt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class MixinBlockItem {

    @Inject(method = "place", at = @At("HEAD"))
    public void place(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getPlayer() instanceof ServerPlayer) {
            ComplexSpawnPointsKt.syncBlockPlace((BlockItem) (Object) this, context);
        }
    }
}
