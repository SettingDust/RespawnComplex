package me.settingdust.respawncomplex

import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent
import dev.onyxstudios.cca.api.v3.entity.PlayerComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.SetSerializer
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import kotlin.streams.asSequence

val ServerPlayer.activatedRespawnPoints: MutableSet<Location>
    get() = RespawnComplex.Components.COMPLEX_RESPAWNING[this].activated

fun ServerPlayer.activate(location: Location): Boolean {
    val result = RespawnComplex.Components.COMPLEX_RESPAWNING[this].activated.add(location)
    if (
        result &&
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

val respawnPointBlockTag by lazy {
    TagKey.create(Registries.BLOCK, RespawnComplex.location("respawn_point")!!)
}

@OptIn(ExperimentalSerializationApi::class)
@Suppress("UnstableApiUsage")
data class ComplexRespawningComponent(private val player: Player) :
    PlayerComponent<ComplexRespawningComponent>, ServerTickingComponent {
    private var _activated = mutableSetOf<Location>()
    private val locationSetSerializer by lazy {
        SetSerializer(Location.Serializer(serverPlayer!!.server))
    }
    val activated: MutableSet<Location>
        get() = _activated

    val serverPlayer: ServerPlayer?
        get() = player as? ServerPlayer

    private var posCache = BlockPos.ZERO

    init {
        EntitySleepEvents.ALLOW_SETTING_SPAWN.register { _, _ ->
            return@register false
        }

        UseBlockCallback.EVENT.register { player, level, _, hitResult ->
            if (level !is ServerLevel) return@register InteractionResult.PASS
            if (player != this.serverPlayer) return@register InteractionResult.PASS
            if (hitResult.type != HitResult.Type.BLOCK) return@register InteractionResult.PASS
            val pos = hitResult.blockPos
            val state = level.getBlockState(pos)
            val block = state.block
            if (
                RespawnComplex.config.enableSync &&
                    state.`is`(respawnPointBlockTag) &&
                    (block !is ComplexSpawnable ||
                        block.`respawnComplex$isValid`(level, pos, state))
            )
                level.complexSpawnPoints.add(pos)
            if (level.complexSpawnPoints.contains(pos))
                serverPlayer!!.activate(Location(level, pos))
            InteractionResult.PASS
        }
    }

    override fun shouldCopyForRespawn(
        lossless: Boolean,
        keepInventory: Boolean,
        sameCharacter: Boolean
    ) = true

    override fun readFromNbt(tag: CompoundTag) {
        if (serverPlayer == null) return
        _activated =
            minecraftTag.decodeFromTag(locationSetSerializer, tag.get("activated")!!).toMutableSet()
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
        val level = serverPlayer!!.level() as ServerLevel
        level.complexSpawnPoints
            .filter {
                it.distSqr(serverPlayer!!.blockPosition()) <=
                    RespawnComplex.config.activationRangeSqr
            }
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

    private fun spawnAtSharedOverworldSpawn(): Location {
        val overworldLevel = serverPlayer!!.server.overworld()
        val sharedSpawnPos = overworldLevel.sharedSpawnPos
        RespawnComplex.logger.debug(
            "Getting {} spawn point {}",
            overworldLevel.dimension().location(),
            sharedSpawnPos.toShortString()
        )
        return Location(overworldLevel, sharedSpawnPos)
    }

    private fun spawnsInOverworld(): Sequence<Location> {
        val overworldLevel = serverPlayer!!.server.overworld()
        val activatedSpawnsInOverworld by lazy {
            activated
                .asSequence()
                .filter { it.level == overworldLevel }
                .filter {
                    val state = it.level.getBlockState(it.pos)
                    val block = state.block
                    block !is ComplexSpawnable ||
                        block.`respawnComplex$isValid`(it.level, it.pos, state)
                }
        }
        val spawnsInOverworld by lazy {
            overworldLevel.complexSpawnPoints
                .asSequence()
                .filter {
                    val state = overworldLevel.getBlockState(it)
                    val block = state.block
                    block !is ComplexSpawnable ||
                        block.`respawnComplex$isValid`(overworldLevel, it, state)
                }
                .map { Location(overworldLevel, it) }
        }

        RespawnComplex.logger.debug(
            "Getting {} spawn points in {}",
            if (RespawnComplex.config.enableActivation) "activated" else "all",
            overworldLevel.dimension().location()
        )

        return if (RespawnComplex.config.enableActivation) activatedSpawnsInOverworld
        else spawnsInOverworld
    }

    private fun spawnsInDeathLevel(deathLocation: Location): Sequence<Location> {
        val activatedSpawnsInDeathLevel by lazy {
            activated
                .asSequence()
                .filter { it.level == deathLocation.level }
                .filter {
                    val state = it.level.getBlockState(it.pos)
                    val block = state.block
                    block !is ComplexSpawnable ||
                        block.`respawnComplex$isValid`(it.level, it.pos, state)
                }
                .sortedBy { it.pos.distSqr(deathLocation.pos) }
        }
        val spawnsInDeathLevel by lazy {
            deathLocation.level.complexSpawnPoints
                .asSequence()
                .filter {
                    val state = deathLocation.level.getBlockState(it)
                    val block = state.block
                    block !is ComplexSpawnable ||
                        block.`respawnComplex$isValid`(deathLocation.level, it, state)
                }
                .sortedBy { it.distSqr(deathLocation.pos) }
                .map { Location(deathLocation.level, it) }
        }

        RespawnComplex.logger.debug(
            "Getting {} spawn points in {}",
            if (RespawnComplex.config.enableActivation) "activated" else "all",
            deathLocation.level.dimension().location()
        )

        return if (RespawnComplex.config.enableActivation) activatedSpawnsInDeathLevel
        else spawnsInDeathLevel
    }

    fun complexRespawnPoint(deathLocation: Location): Location {
        activated.removeIf {
            it.level == deathLocation.level && it.pos !in deathLocation.level.complexSpawnPoints
        }

        val possibleSpawns =
            spawnsInDeathLevel(deathLocation) +
                if (serverPlayer!!.server.overworld() == deathLocation.level) emptySequence()
                else spawnsInOverworld() + spawnAtSharedOverworldSpawn()

        val result =
            possibleSpawns
                .map {
                    RespawnComplex.logger.debug(
                        "Considering the point at {} in {}",
                        it.pos.toShortString(),
                        it.level.dimension().location()
                    )
                    it to availableSpaceFromPos(it)
                }
                .filter {
                    if (it.second == null) {
                        RespawnComplex.logger.debug(
                            "No safe point for {} in {}",
                            it.first.pos.toShortString(),
                            it.first.level.dimension().location()
                        )
                    }
                    it.second != null
                }
                .first()
        val location = result.first

        RespawnComplex.logger.debug(
            "Found the point at {} in {}",
            location.pos.toShortString(),
            location.level.dimension().location()
        )

        val blockState = location.level.getBlockState(location.pos)
        val block = blockState.block
        if (block is ComplexSpawnable)
            block.`respawnComplex$onRespawn`(location.level, location.pos, blockState)

        return result.second!!
    }

    private fun availableSpaceFromPos(location: Location, radius: Int = 3): Location? {
        val level = location.level
        val size = radius * 2
        val nearPoses = BlockPos.withinManhattan(location.pos, size, size, size).asSequence()
        val immutablePoses = nearPoses.map { it.immutable() }
        val solidPoses =
            immutablePoses.filter {
                val blockState = level.getBlockState(it.below())
                blockState.blocksMotion()
            }
        val safePoses =
            solidPoses.filter {
                val blocksAbove = level.getBlockStates(AABB(it, it.above())).asSequence()
                val enoughSpace =
                    blocksAbove.all { state -> state.block.isPossibleToRespawnInThis(state) }
                enoughSpace
            }
        val shuffledSafeBlocks = safePoses.take(16).shuffled()
        val firstPos = shuffledSafeBlocks.firstOrNull()
        return firstPos?.let { Location(level, it) }
    }
}
