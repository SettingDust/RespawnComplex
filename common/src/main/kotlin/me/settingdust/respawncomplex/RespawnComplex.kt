package me.settingdust.respawncomplex

import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

object RespawnComplex {
    const val MOD_ID = "respawn_complex"
    val logger = LoggerFactory.getLogger(RespawnComplex::class.java)
    fun init() {
    }

    fun location(path: String): ResourceLocation? {
        val converted = path.split(":")
        if (converted.size == 1) return ResourceLocation.tryBuild(MOD_ID, path)
        return ResourceLocation.tryParse(path)
    }
}
