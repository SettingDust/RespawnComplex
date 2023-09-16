package me.settingdust.respawncomplex.mixin;

import me.settingdust.respawncomplex.PlayerBreakBlocksEvent;
import me.settingdust.respawncomplex.TransactionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class MixinServerPlayerGameMode {
    @Shadow @Final protected ServerPlayer player;

    @Shadow protected ServerLevel level;

    @Inject(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;isCreative()Z", shift = At.Shift.BEFORE))
    private void respawncomplex$invokePlayerBreakBlocksEvent(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        PlayerBreakBlocksEvent.Context context = PlayerBreakBlocksEvent.Companion.getContext();
        if (context == null) return;
        if (context.getLevel() != level) return;
        if (context.getPlayer() != player) return;
        if (context.getBlockPoses().isEmpty()) return;
        TransactionEvents.PLAYER_BREAK_BLOCKS.invoker()
                .breakBlocks(context.getLevel(), context.getPlayer(), context.getBlockPoses());
        PlayerBreakBlocksEvent.setContext(null);
    }
}
