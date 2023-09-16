package me.settingdust.respawncomplex

import dev.onyxstudios.cca.api.v3.component.Component
import kotlinx.serialization.ExperimentalSerializationApi
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import settingdust.kinecraft.serialization.format.tag.decodeFromTag
import settingdust.kinecraft.serialization.format.tag.encodeToTag

val Level.complexSpawnPoints: MutableSet<BlockPos>
    get() = RespawnComplex.Components.COMPLEX_RESPAWN_POINTS[this].spawnPoints

internal fun ServerLevel.syncBlockPlace(pos: BlockPos, oldState: BlockState, newState: BlockState) {
    if (!RespawnComplex.config.enableSync
        || newState.isAir
        || !newState.`is`(respawnPointBlockTag)
    ) return
    val block = newState.block
    if (block is ComplexSpawnable && !block.`respawnComplex$isValid`(
            level,
            pos,
            newState
        )
    ) return
    if (!complexSpawnPoints.add(pos)) return
    RespawnComplex.logger.debug("Syncing block placing from block state change at {}", pos)
}

internal fun ServerLevel.syncBlockPlace(pos: BlockPos, player: ServerPlayer, state: BlockState) {
    if (!RespawnComplex.config.enableSync) return
    if (!state.`is`(respawnPointBlockTag)) return
    val block = state.block
    if (block is ComplexSpawnable && !block.`respawnComplex$isValid`(
            this,
            pos,
            state
        )
    ) return
    if (!complexSpawnPoints.add(pos)) return
    RespawnComplex.logger.debug("Syncing block placing from player {} at {}", player.name, pos)
    player.activate(Location(this, pos))
}

internal fun ServerLevel.syncBlockBreak(pos: BlockPos, oldState: BlockState, newState: BlockState) {
    if (!RespawnComplex.config.enableSync) return
    if (!newState.isAir) return
    if (oldState.isAir) return
    if (complexSpawnPoints.remove(pos)) {
        RespawnComplex.logger.debug("Syncing block breaking at {}", pos)
    }
}

@OptIn(ExperimentalSerializationApi::class)
data class ComplexSpawnPointsComponent(private val level: Level) : Component {
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
}
