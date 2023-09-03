package me.settingdust.respawncomplex

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import me.settingdust.respawncomplex.serialization.BlockPosAsLongSerializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import net.luckperms.api.LuckPermsProvider
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.server.level.ServerLevel
import settingdust.kinecraft.serialization.format.tag.MinecraftTag

@OptIn(ExperimentalSerializationApi::class)
internal val minecraftTag = MinecraftTag {
    serializersModule = SerializersModule {
        contextual(BlockPosAsLongSerializer)
    }
}

private val luckperms: Boolean = FabricLoader.getInstance().isModLoaded("luckperms")

fun initFabricLike() {
    Config()
    val rootCommandBuilder = literal<CommandSourceStack>("spawn")
        .requires { it.player != null }
        .requires {
            if (luckperms) {
                it.player?.let { player ->
                    LuckPermsProvider.get().userManager.getUser(player.uuid)?.cachedData?.permissionData?.checkPermission(
                        "respawncomplex.command.spawn",
                    )?.asBoolean()
                } ?: true
            } else {
                true
            }
        }
        .executes { context ->
            val player = context.source.player!!
            val level = player.level()
            val location = player.complexRespawnPoint(Location(level as ServerLevel, player.blockPosition()))
            player.teleportTo(location.pos.x + 0.5, location.pos.y.toDouble(), location.pos.z + 0.5)
            1
        }
    val setCommand = literal<CommandSourceStack>("set")
        .requires { it.player != null }
        .requires {
            if (luckperms) {
                it.player?.let { player ->
                    LuckPermsProvider.get().userManager.getUser(player.uuid)?.cachedData?.permissionData?.checkPermission(
                        "respawncomplex.admin.command.set",
                    )?.asBoolean()
                } ?: true
            } else {
                it.player?.hasPermissions(3) ?: true
            }
        }
        .executes { context ->
            val player = context.source.player!!
            player.level().complexSpawnPoints.add(player.blockPosition())
            1
        }.build()
    val removeCommand = literal<CommandSourceStack>("remove").then(argument("location", blockPos()))
        .requires {
            if (luckperms) {
                it.player?.let { player ->
                    LuckPermsProvider.get().userManager.getUser(player.uuid)?.cachedData?.permissionData?.checkPermission(
                        "respawncomplex.admin.command.remove",
                    )?.asBoolean()
                } ?: true
            } else {
                it.player?.hasPermissions(3) ?: true
            }
        }
        .executes { context ->
            val player = context.source.player!!
            val level = player.level()
            val pos = try {
                context.getArgument("location", BlockPos::class.java)
            } catch (_: IllegalArgumentException) {
                player.blockPosition()
            }
            try {
                val closestPos = level.complexSpawnPoints.first { it.distSqr(pos) < 4 }
                level.complexSpawnPoints.remove(closestPos)
                context.source.sendSuccess(
                    {
                        Component.translatable(
                            "respawn_complex.command.remove.success",
                            Component.literal(closestPos.toShortString())
                                .withStyle(
                                    Style.EMPTY.withColor(ChatFormatting.GREEN)
                                        .withClickEvent(
                                            ClickEvent(
                                                ClickEvent.Action.SUGGEST_COMMAND,
                                                "/tp ${pos.x} ${pos.y} ${pos.z}",
                                            ),
                                        )
                                        .withHoverEvent(
                                            HoverEvent(
                                                HoverEvent.Action.SHOW_TEXT,
                                                Component.translatable("respawn_complex.command.click_to_teleport"),
                                            ),
                                        ),
                                ),
                        )
                    },
                    true,
                )
            } catch (e: NoSuchElementException) {
                context.source.sendFailure(
                    Component.translatable("respawn_complex.command.remove.failure")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)),
                )
                return@executes 0
            }
            1
        }.build()
    val listCommand = literal<CommandSourceStack>("list")
        .requires {
            if (luckperms) {
                it.player?.let { player ->
                    LuckPermsProvider.get().userManager.getUser(player.uuid)?.cachedData?.permissionData?.checkPermission(
                        "respawncomplex.admin.command.list",
                    )?.asBoolean()
                } ?: true
            } else {
                it.player?.hasPermissions(3) ?: true
            }
        }
        .executes { context ->
            val player = context.source.player!!
            val level = player.level()
            val spawnPoints = level.complexSpawnPoints
            if (spawnPoints.isEmpty()) {
                context.source.sendSuccess(
                    {
                        Component.translatable("respawn_complex.command.list.empty")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.RED))
                    },
                    false,
                )
                return@executes 0
            }
            val component = Component.empty()
            spawnPoints.forEach { pos ->
                component.append(
                    Component.literal(pos.toShortString())
                        .withStyle(
                            Style.EMPTY.withColor(ChatFormatting.GREEN)
                                .withClickEvent(
                                    ClickEvent(
                                        ClickEvent.Action.SUGGEST_COMMAND,
                                        "/tp ${pos.x} ${pos.y} ${pos.z}",
                                    ),
                                )
                                .withHoverEvent(
                                    HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("respawn_complex.command.click_to_teleport"),
                                    ),
                                ),
                        ),
                )
                component.append("\n")
            }
            context.source.sendSuccess({ component }, false)
            1
        }.build()

    CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
        dispatcher
            .register(
                rootCommandBuilder
                    .then(setCommand)
                    .then(removeCommand)
                    .then(listCommand),
            )
    }
}
