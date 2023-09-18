package me.settingdust.respawncomplex.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import me.settingdust.respawncomplex.ComplexRespawningKt;
import me.settingdust.respawncomplex.Location;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerList.class)
public class MixinPlayerList {
    @Inject(method = "respawn", at = @At("HEAD"))
    private void respawncomplex$injectComplexPoint(
            ServerPlayer player,
            boolean keepEverything,
            CallbackInfoReturnable<ServerPlayer> cir,
            @Share("complexPoint") LocalRef<Location> complexPointRef) {
        final var complexPoint =
                ComplexRespawningKt.complexRespawnPoint(player, new Location(player.serverLevel(), player.getOnPos()));
        player.setRespawnPosition(
                complexPoint.getLevel().dimension(), complexPoint.getPos(), player.yHeadRot, false, false);
        complexPointRef.set(complexPoint);
    }

    @WrapOperation(
            method = "respawn",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/player/Player;findRespawnPositionAndUseSpawnBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;FZZ)Ljava/util/Optional;"))
    private static Optional<Vec3> respawncomplex$findComplexPoint(
            ServerLevel serverLevel,
            BlockPos spawnBlockPos,
            float playerOrientation,
            boolean isRespawnForced,
            boolean respawnAfterWinningTheGame,
            Operation<Optional<Vec3>> original,
            @Share("complexPoint") LocalRef<Location> complexPointRef) {
        return Optional.of(Vec3.atBottomCenterOf(complexPointRef.get().getPos()));
    }
}
