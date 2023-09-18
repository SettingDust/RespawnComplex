package me.settingdust.respawncomplex

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

/**
 * Applying to block for complex condition before spawning
 */
interface ComplexSpawnable {
    @Suppress("FunctionName")
    fun `respawnComplex$isValid`(level: Level, pos: BlockPos, state: BlockState): Boolean

    fun `respawnComplex$onRespawn`(level: Level, pos: BlockPos, state: BlockState)

    fun `respawnComplex$onActivate`(level: Level, pos: BlockPos, state: BlockState) {}
}
