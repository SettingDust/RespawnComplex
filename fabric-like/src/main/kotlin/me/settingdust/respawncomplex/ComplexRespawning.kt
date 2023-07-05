package me.settingdust.respawncomplex

import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent
import dev.onyxstudios.cca.api.v3.entity.PlayerComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.SetSerializer
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.RespawnAnchorBlock
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import kotlin.streams.asSequence

val ServerPlayer.activatedRespawnPoints: MutableSet<Location>
    get() = RespawnComplex.Components.COMPLEX_RESPAWNING[this].activated

fun ServerPlayer.activate(location: Location): Boolean {
    val result = RespawnComplex.Components.COMPLEX_RESPAWNING[this].activated.add(location)
    if (result &&
        RespawnComplex.config.enableActivation &&
        RespawnComplex.config.sendActivationMessage
    ) {
        sendSystemMessage(
            Component.translatable(
                "respawn_complex.message.activated",
                location.pos,
            ),
        )
    }
    return result
}

fun ServerPlayer.complexRespawnPoint(deathLocation: Location): Location =
    RespawnComplex.Components.COMPLEX_RESPAWNING[this].complexRespawnPoint(deathLocation)

val respawnPointBlockTag by lazy { TagKey.create(Registry.BLOCK_REGISTRY, RespawnComplex.location("respawn_point")!!) }

@OptIn(ExperimentalSerializationApi::class)
@Suppress("UnstableApiUsage")
data class ComplexRespawningComponent(private val player: Player) :
    PlayerComponent<ComplexRespawningComponent>, ServerTickingComponent {
    private var _activated = mutableSetOf<Location>()
    private val locationSetSerializer by lazy { SetSerializer(Location.Serializer(serverPlayer!!.server)) }
    val activated: MutableSet<Location>
        get() = _activated
    val serverPlayer: ServerPlayer?
        get() = player as? ServerPlayer
    private var posCache = BlockPos.ZERO

    init {
        UseBlockCallback.EVENT.register { player, world, _, hitResult ->
            if (world !is ServerLevel) return@register InteractionResult.PASS
            if (player != this.serverPlayer) return@register InteractionResult.PASS
            if (hitResult.type != HitResult.Type.BLOCK) return@register InteractionResult.PASS
            val pos = hitResult.blockPos
            val state = world.getBlockState(pos)
            if (RespawnComplex.config.enableSync && !world.complexSpawnPoints.contains(pos)) {
                if (state.`is`(respawnPointBlockTag)) {
                    var success = false
                    when (state.block) {
                        is RespawnAnchorBlock ->
                            if (RespawnAnchorBlock.canSetSpawn(world) &&
                                state.getValue(RespawnAnchorBlock.CHARGE) > 0
                            ) {
                                success = true
                            }

                        else -> success = true
                    }
                    if (success && serverPlayer!!.activate(Location(world, pos))) {
                        return@register InteractionResult.SUCCESS
                    }
                }
            }
            if (state.`is`(BlockTags.BEDS) || state.block is RespawnAnchorBlock) {
                InteractionResult.FAIL
            } else {
                InteractionResult.PASS
            }
        }
    }

    override fun shouldCopyForRespawn(lossless: Boolean, keepInventory: Boolean, sameCharacter: Boolean) = true

    override fun copyForRespawn(
        original: ComplexRespawningComponent,
        lossless: Boolean,
        keepInventory: Boolean,
        sameCharacter: Boolean,
    ) {
        if (serverPlayer == null) return
        super.copyForRespawn(original, lossless, keepInventory, sameCharacter)
        val respawnPoint = complexRespawnPoint(
            Location(
                original.serverPlayer!!.level as ServerLevel,
                original.serverPlayer!!.blockPosition(),
            ),
        )
        serverPlayer!!.setRespawnPosition(serverPlayer!!.level.dimension(), respawnPoint.pos, 0f, false, false)
        serverPlayer!!.moveTo(
            respawnPoint.pos.x + 0.5,
            respawnPoint.pos.y.toDouble(),
            respawnPoint.pos.z + 0.5,
        )
    }

    override fun readFromNbt(tag: CompoundTag) {
        if (serverPlayer == null) return
        _activated = minecraftTag.decodeFromTag(locationSetSerializer, tag.get("activated")!!).toMutableSet()
    }

    override fun writeToNbt(tag: CompoundTag) {
        if (serverPlayer == null) return
        tag.put("activated", minecraftTag.encodeToTag(locationSetSerializer, _activated))
    }

    override fun serverTick() {
        if (RespawnComplex.config.enableActivation.not()) return
        if (RespawnComplex.config.activateMethod != ActivateMethod.MOVING) return
        if (posCache == serverPlayer!!.blockPosition()) return
        posCache = serverPlayer!!.blockPosition()
        val level = serverPlayer!!.level as ServerLevel
        level.complexSpawnPoints
            .filter { it.distSqr(serverPlayer!!.blockPosition()) <= RespawnComplex.config.activationRangeSqr }
            .filter { serverPlayer!!.activatedRespawnPoints.contains(Location(level, it)).not() }
            .forEach {
                serverPlayer!!.activatedRespawnPoints.add(Location(level, it))
                if (RespawnComplex.config.sendActivationMessage) {
                    serverPlayer!!.sendSystemMessage(
                        Component.translatable("respawncomplex.message.activated"),
                    )
                }
            }
    }

    fun complexRespawnPoint(deathLocation: Location): Location {
        activated.removeIf { it.level == deathLocation.level && it.pos !in deathLocation.level.complexSpawnPoints }
        val availablePointFromActivated by lazy {
            activated
                .filter { it.level == deathLocation.level }
                .sortedBy { it.pos.distSqr(deathLocation.pos) }
                .firstNotNullOfOrNull { availableSpaceFromPos(it) }
        }
        val availablePointFromLevel by lazy {
            deathLocation.level.complexSpawnPoints
                .sortedBy { it.distSqr(deathLocation.pos) }
                .firstNotNullOfOrNull { availableSpaceFromPos(Location(deathLocation.level, it)) }
        }
        val availableComplexPoint =
            if (RespawnComplex.config.enableActivation) {
                availablePointFromActivated
            } else {
                availablePointFromLevel
            }

        val overworldLevel = serverPlayer!!.server.overworld()
        val availableOverworldSharedPoint by lazy {
            availableSpaceFromPos(
                Location(
                    overworldLevel,
                    overworldLevel.sharedSpawnPos,
                ),
            )
        }

        return availableComplexPoint
            ?: availableOverworldSharedPoint ?: Location(
            overworldLevel,
            overworldLevel.sharedSpawnPos,
        )
    }

    private fun availableSpaceFromPos(location: Location, radius: Int = 3): Location? {
        val level = location.level
        val size = radius * 2
        val nearPoses = BlockPos.withinManhattan(location.pos, size, size, size).asSequence()
        val immutablePoses = nearPoses.map { it.immutable() }
        val solidPoses = immutablePoses.filter { level.getBlockState(it.below()).material.blocksMotion() }
        val safePoses = solidPoses.filter {
            val blocksAbove = level.getBlockStates(AABB(it, it.above())).asSequence()
            val enoughSpace = blocksAbove.all { state -> !state.material.blocksMotion() }
            enoughSpace
        }
        val shuffledSafeBlocks = safePoses.take(16).shuffled()
        val firstPos = shuffledSafeBlocks.firstOrNull()
        return firstPos?.let { Location(level, it) }
    }
}
