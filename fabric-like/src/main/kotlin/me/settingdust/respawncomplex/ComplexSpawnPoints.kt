package me.settingdust.respawncomplex

import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent
import kotlinx.serialization.ExperimentalSerializationApi
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import settingdust.tag.serialization.decodeFromTag
import settingdust.tag.serialization.encodeToTag

val Level.complexSpawnPoints: MutableSet<BlockPos>
    get() = RespawnComplex.Components.COMPLEX_RESPAWN_POINTS[this].spawnPoints

@OptIn(ExperimentalSerializationApi::class)
data class ComplexSpawnPointsComponent(private val level: Level) : ServerTickingComponent {
    private var _spawnPoints = mutableSetOf<BlockPos>()
    val spawnPoints: MutableSet<BlockPos>
        get() = _spawnPoints

    val serverLevel: ServerLevel?
        get() = level as? ServerLevel

    override fun readFromNbt(tag: CompoundTag) {
        _spawnPoints = minecraftTag.decodeFromTag(tag.get("spawnPoints")!!)
    }

    override fun writeToNbt(tag: CompoundTag) {
        tag.put("spawnPoints", minecraftTag.encodeToTag(_spawnPoints))
    }

    override fun serverTick() {
        if (RespawnComplex.config.enableActivation.not()) return
        if (RespawnComplex.config.activateMethod != ActivateMethod.MOVING) return
        serverLevel!!.players().forEach { player ->
            spawnPoints
                .filter { it.distSqr(player.blockPosition()) <= RespawnComplex.config.activationRangeSqr }
                .filter { player.activatedRespawnPoints.contains(Location(serverLevel!!, it)).not() }
                .forEach {
                    player.activatedRespawnPoints.add(Location(serverLevel!!, it))
                    if (RespawnComplex.config.sendActivationMessage) {
                        player.sendSystemMessage(
                            Component.translatable("respawncomplex.message.activated"),
                        )
                    }
                }
        }
    }
}
