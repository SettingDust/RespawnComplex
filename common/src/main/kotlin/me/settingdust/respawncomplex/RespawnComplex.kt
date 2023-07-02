package me.settingdust.respawncomplex

import net.minecraft.resources.ResourceLocation

object RespawnComplex {
    const val MOD_ID = "respawn_complex"
    fun init() {
    }

    fun location(path: String): ResourceLocation? {
        val converted = path.split(":")
        if (converted.size == 1) return ResourceLocation.tryBuild(MOD_ID, path)
        return ResourceLocation.tryParse(path)
    }
}
