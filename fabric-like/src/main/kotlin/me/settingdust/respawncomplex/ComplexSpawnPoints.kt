package me.settingdust.respawncomplex

import dev.onyxstudios.cca.api.v3.component.Component
import kotlinx.serialization.ExperimentalSerializationApi
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.RespawnAnchorBlock
import settingdust.kinecraft.serialization.format.tag.decodeFromTag
import settingdust.kinecraft.serialization.format.tag.encodeToTag

val Level.complexSpawnPoints: MutableSet<BlockPos>
    get() = RespawnComplex.Components.COMPLEX_RESPAWN_POINTS[this].spawnPoints

internal fun BlockItem.syncBlockPlace(context: BlockPlaceContext) {
    if (!RespawnComplex.config.enableSync) return
    val blockState = context.level.getBlockState(context.clickedPos)
    if (blockState.`is`(respawnPointBlockTag)) {
        var success = false
        when (block) {
            is RespawnAnchorBlock ->
                if (RespawnAnchorBlock.canSetSpawn(context.level) &&
                    (block.getStateForPlacement(context)?.getValue(RespawnAnchorBlock.CHARGE) ?: 0) > 0
                ) {
                    success = true
                }

            else -> success = true
        }
        if (success) {
            val level = context.level as ServerLevel
            level.complexSpawnPoints.add(context.clickedPos)
            (context.player as ServerPlayer).activate(Location(level, context.clickedPos))
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
        PlayerBlockBreakEvents.AFTER.register { level, player, blockPos, blockState, blockEntity ->
            if (level !is ServerLevel) return@register
            if (player !is ServerPlayer) return@register
            if (serverLevel != level) return@register
            _spawnPoints.remove(blockPos)
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
