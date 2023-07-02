package me.settingdust.respawncomplex

import dev.onyxstudios.cca.api.v3.component.ComponentKey
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer
import dev.onyxstudios.cca.api.v3.world.WorldComponentFactoryRegistry
import dev.onyxstudios.cca.api.v3.world.WorldComponentInitializer
import me.settingdust.respawncomplex.ComponentKeys.COMPLEX_RESPAWNING
import me.settingdust.respawncomplex.ComponentKeys.COMPLEX_RESPAWN_POINTS

val RespawnComplex.Components: ComponentKeys
    get() = ComponentKeys

object ComponentKeys {
    val COMPLEX_RESPAWNING: ComponentKey<ComplexRespawningComponent> =
        ComponentRegistry.getOrCreate(RespawnComplex.location("complex_respawning")!!, ComplexRespawningComponent::class.java)
    val COMPLEX_RESPAWN_POINTS: ComponentKey<ComplexSpawnPointsComponent> =
        ComponentRegistry.getOrCreate(RespawnComplex.location("complex_respawn_points")!!, ComplexSpawnPointsComponent::class.java)
}

@Suppress("unused")
object Components : EntityComponentInitializer, WorldComponentInitializer {
    @Suppress("UnstableApiUsage")
    override fun registerEntityComponentFactories(registry: EntityComponentFactoryRegistry) {
        registry.registerForPlayers(COMPLEX_RESPAWNING, ::ComplexRespawningComponent)
    }

    override fun registerWorldComponentFactories(registry: WorldComponentFactoryRegistry) {
        registry.register(COMPLEX_RESPAWN_POINTS, ::ComplexSpawnPointsComponent)
    }
}
