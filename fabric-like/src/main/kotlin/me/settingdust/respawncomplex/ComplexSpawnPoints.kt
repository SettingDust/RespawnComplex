package me.settingdust.respawncomplex

import dev.onyxstudios.cca.api.v3.component.Component
import kotlinx.serialization.ExperimentalSerializationApi
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import settingdust.tag.serialization.decodeFromTag
import settingdust.tag.serialization.encodeToTag

val Level.complexSpawnPoints: MutableSet<BlockPos>
    get() = RespawnComplex.Components.COMPLEX_RESPAWN_POINTS[this].spawnPoints

internal fun ServerLevel.syncBlockPlace(player: ServerPlayer, pos: BlockPos, block: Block) {
    if (!RespawnComplex.config.enableSync) return
    if (block.builtInRegistryHolder().`is`(respawnPointBlockTag)) {
        RespawnComplex.Components.COMPLEX_RESPAWN_POINTS[this].spawnPoints.add(pos)
        player.activate(Location(this, pos))
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
        }
    }

    override fun readFromNbt(tag: CompoundTag) {
        _spawnPoints = minecraftTag.decodeFromTag(tag.get("spawnPoints")!!)
    }

    override fun writeToNbt(tag: CompoundTag) {
        tag.put("spawnPoints", minecraftTag.encodeToTag(_spawnPoints))
    }
}
