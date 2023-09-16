package me.settingdust.respawncomplex

import net.fabricmc.fabric.api.event.EventFactory
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

object TransactionEvents {
    @JvmField
    val PLAYER_BREAK_BLOCKS = EventFactory.createArrayBacked(PlayerBreakBlocksEvent::class.java) { listeners ->
        PlayerBreakBlocksEvent { level, player, blockPoses ->
            for (event in listeners) {
                event.breakBlocks(level, player, blockPoses)
            }
        }
    }!!
}

fun interface PlayerBreakBlocksEvent {
    companion object {
        @JvmStatic
        var context: Context? = null

//        init {
            /**
             * Needn't since I'm using [ServerLevel#onBlockStateChange]
             */
//            PlayerBlockBreakEvents.BEFORE.register { level, player, _, _, _ ->
//                context = Context(level, player)
//                true
//            }
//            PlayerBlockBreakEvents.CANCELED.register { level, player, _, _, _ ->
//                if (context == null) return@register
//                if (context!!.level != level) return@register
//                if (context!!.player != player) return@register
//                context = null
//            }
            /**
             * fabric event won't be called when break bed since it's removed by neighbor update instead of the line
             * [me.settingdust.respawncomplex.mixin.MixinServerPlayerGameMode]
             */
//            PlayerBlockBreakEvents.AFTER.register { level, player, _, _, _ ->
//                if (context == null) return@register
//                if (context!!.level != level) return@register
//                if (context!!.player != player) return@register
//                if (context!!.blockPoses.isEmpty()) return@register
//                TransactionEvents.PLAYER_BREAK_BLOCKS.invoker()
//                    .breakBlocks(context!!.level, context!!.player, context!!.blockPoses)
//                context = null
//            }
//        }
    }

    data class Context(val level: Level, val player: Player, val blockPoses: MutableSet<BlockPos> = hashSetOf())

    fun breakBlocks(leve: Level, player: Player, blockPoses: Set<BlockPos>)
}
