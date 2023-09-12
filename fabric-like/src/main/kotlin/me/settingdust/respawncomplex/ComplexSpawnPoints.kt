package me.settingdust.respawncomplex

import dev.onyxstudios.cca.api.v3.component.Component
import kotlinx.serialization.ExperimentalSerializationApi
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.RespawnAnchorBlock
import net.minecraft.world.level.block.state.BlockState
import settingdust.kinecraft.serialization.format.tag.decodeFromTag
import settingdust.kinecraft.serialization.format.tag.encodeToTag

val Level.complexSpawnPoints: MutableSet<BlockPos>
    get() = RespawnComplex.Components.COMPLEX_RESPAWN_POINTS[this].spawnPoints

internal fun BlockItem.syncBlockPlace(pos: BlockPos, level: ServerLevel, player: ServerPlayer, state: BlockState) {
    if (!RespawnComplex.config.enableSync) return
    if (state.`is`(respawnPointBlockTag)) {
        var success = false
        when (block) {
            is RespawnAnchorBlock ->
                if (RespawnAnchorBlock.canSetSpawn(level) &&
                    (state.getValue(RespawnAnchorBlock.CHARGE) ?: 0) > 0
                ) {
                    success = true
                }

            else -> success = true
        }
        if (success) {
            RespawnComplex.logger.debug("Syncing block placing")
            level.complexSpawnPoints.add(pos)
            player.activate(Location(level, pos))
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
data class ComplexSpawnPointsComponent(private val level: Level) : Component {
    private var _spawnPoints = mutableSetOf<BlockPos>()
    val spawnPoints: MutableSet<BlockPos>
        get() = _spawnPoints

    val serverLevel: ServerLevel?
        get() = level as? ServerLevel

    init {
        PlayerBlockBreakEvents.AFTER.register { level, player, blockPos, _, _ ->
            if (level !is ServerLevel) return@register
            if (player !is ServerPlayer) return@register
            if (serverLevel != level) return@register
            _spawnPoints.remove(blockPos)
            RespawnComplex.logger.debug("Syncing block breaking")
            level.server.playerList.players.forEach {
                it.activatedRespawnPoints.remove(
                    Location(
                        serverLevel!!,
                        blockPos,
                    ),
                )
            }
        }
    }

    override fun readFromNbt(tag: CompoundTag) {
        _spawnPoints = minecraftTag.decodeFromTag(tag.get("spawnPoints")!!)
    }

    override fun writeToNbt(tag: CompoundTag) {
        tag.put("spawnPoints", minecraftTag.encodeToTag(_spawnPoints))
    }
}
