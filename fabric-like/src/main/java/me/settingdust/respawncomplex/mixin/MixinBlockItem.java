package me.settingdust.respawncomplex.mixin;

import me.settingdust.respawncomplex.ComplexSpawnPointsKt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class MixinBlockItem {

    @Shadow public abstract Block getBlock();

    @Inject(method = "place", at = @At("HEAD"))
    public void place(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            var level = player.getLevel();
            var pos = context.getClickedPos();
            ComplexSpawnPointsKt.syncBlockPlace(level, player, pos, getBlock());
        }
    }
}
