package me.settingdust.respawncomplex

import org.quiltmc.loader.api.ModContainer
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer

object Entrypoint : ModInitializer {
    override fun onInitialize(mod: ModContainer) {
        initFabricLike()
    }
}
