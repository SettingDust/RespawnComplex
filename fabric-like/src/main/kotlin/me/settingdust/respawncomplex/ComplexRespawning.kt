package me.settingdust.respawncomplex

import dev.onyxstudios.cca.api.v3.entity.PlayerComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.SetSerializer
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.streams.asSequence

val ServerPlayer.activatedRespawnPoints: MutableSet<Location>
    get() = RespawnComplex.Components.COMPLEX_RESPAWNING[this].activated

fun ServerPlayer.complexRespawnPoint(deathLocation: Location): Location =
    RespawnComplex.Components.COMPLEX_RESPAWNING[this].complexRespawnPoint(deathLocation)

@OptIn(ExperimentalSerializationApi::class)
@Suppress("UnstableApiUsage")
data class ComplexRespawningComponent(private val player: Player) :
    PlayerComponent<ComplexRespawningComponent> {
    private var _activated = mutableSetOf<Location>()
    private val locationSetSerializer by lazy { SetSerializer(Location.Serializer(serverPlayer!!.server)) }
    val activated: MutableSet<Location>
        get() = _activated
    val serverPlayer: ServerPlayer?
        get() = player as? ServerPlayer

    init {
        UseBlockCallback.EVENT.register { player, world, _, hitResult ->
            if (world !is ServerLevel) InteractionResult.PASS
            if (player != this.serverPlayer) InteractionResult.PASS
            if (!RespawnComplex.config.enableActivation || RespawnComplex.config.activateMethod != ActivateMethod.INTERACT) InteractionResult.PASS
            if (world.complexSpawnPoints.contains(hitResult.blockPos)) {
                activated.add(Location(world as ServerLevel, hitResult.blockPos))
                if (RespawnComplex.config.sendActivationMessage) {
                    player.sendSystemMessage(
                        Component.translatable(
                            "respawncomplex.message.activated",
                            hitResult.blockPos,
                        ),
                    )
                }
                return@register InteractionResult.SUCCESS
            }
            InteractionResult.PASS
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
        serverPlayer!!.moveTo(
            respawnPoint.pos.x + 0.5,
            respawnPoint.pos.y + 0.5,
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

    fun complexRespawnPoint(deathLocation: Location): Location {
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

    private fun availableSpaceFromPos(location: Location, radius: Int = 3) =
        BlockPos.betweenClosedStream(
            AABB.ofSize(
                Vec3.atCenterOf(location.pos),
                radius * 2.0,
                radius * 2.0,
                radius * 2.0,
            ),
        )
            .asSequence()
            .firstOrNull {
                location.level.noCollision(AABB(it, it.above(2)))
            }.let { pos ->
                pos?.let { Location(location.level, it) }
            }
}
