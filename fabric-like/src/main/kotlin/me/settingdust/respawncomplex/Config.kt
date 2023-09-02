package me.settingdust.respawncomplex

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

enum class ActivateMethod : NameableEnum {
    INTERACT {
        override fun getDisplayName() = Component.translatable("respawn_complex.option.activate_method.interact")
    },
    MOVING {
        override fun getDisplayName() = Component.translatable("respawn_complex.option.activate_method.moving")
    },
}

val RespawnComplex.config: Config
    get() = Config.Instance

object ConfigModmenuProvider : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
        ConfigScreenFactory { Config.Instance.createScreen(it) }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Config(
    private var _enableActivation: Boolean = true,
    private var _activateMethod: ActivateMethod = ActivateMethod.INTERACT,
    private var _sendActivationMessage: Boolean = true,
    private var _activationRange: Int = 4,
    private var _enableSync: Boolean = true,
) {
    companion object {
        var Instance: Config
            private set

        private val configPath by lazy { FabricLoader.getInstance().configDir / "respawn_complex.json" }

        init {
            try {
                Instance = Json.decodeFromStream<Config>(configPath.inputStream())
            } catch (e: Exception) {
                Instance = Config()
                Instance.save()
            }
        }
    }

    val enableActivation: Boolean
        get() = _enableActivation
    val activateMethod: ActivateMethod
        get() = _activateMethod
    val sendActivationMessage: Boolean
        get() = _sendActivationMessage
    val activationRangeSqr: Int
        get() = _activationRange * _activationRange
    val enableSync: Boolean
        get() = _enableSync

    private val config: YetAnotherConfigLib
        get() = YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("respawn_complex.name"))
            .category(
                ConfigCategory.createBuilder()
                    .name(Component.translatable("respawn_complex.config.category.activation"))
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Component.translatable("respawn_complex.option.enable_activation"))
                            .description(OptionDescription.of(Component.translatable("respawn_complex.option.enable_activation.tooltip")))
                            .binding(true, ::_enableActivation) { _enableActivation = it }
                            .controller { BooleanControllerBuilder.create(it) }
                            .build(),
                    )
                    .option(
                        Option.createBuilder<ActivateMethod>()
                            .name(Component.translatable("respawn_complex.option.activate_method"))
                            .description(
                                OptionDescription.of(
                                    Component.translatable(
                                        "respawn_complex.option.activate_method.tooltip",
                                        ActivateMethod.INTERACT.displayName,
                                        ActivateMethod.MOVING.displayName,
                                    )
                                ),
                            )
                            .binding(ActivateMethod.INTERACT, ::_activateMethod) { _activateMethod = it }
                            .controller {
                                EnumControllerBuilder.create(it)
                            }
                            .build(),
                    )
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Component.translatable("respawn_complex.option.send_activation_message"))
                            .binding(true, ::_sendActivationMessage) { _sendActivationMessage = it }
                            .controller { BooleanControllerBuilder.create(it) }
                            .build(),
                    )
                    .option(
                        Option.createBuilder<Int>()
                            .name(Component.translatable("respawn_complex.option.activation_range"))
                            .description(OptionDescription.of(Component.translatable("respawn_complex.option.activation_range.tooltip")))
                            .binding(4, ::_activationRange) { _activationRange = it }
                            .controller { IntegerSliderControllerBuilder.create(it).range(0, 32).step(1) }
                            .build(),
                    )
                    .build(),
            )
            .category(
                ConfigCategory.createBuilder()
                    .name(Component.translatable("respawn_complex.config.category.sync"))
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Component.translatable("respawn_complex.option.enable_sync"))
                            .description(OptionDescription.of(Component.translatable("respawn_complex.option.enable_sync.tooltip")))
                            .binding(true, ::_enableSync) { _enableSync = it }
                            .controller { BooleanControllerBuilder.create(it) }
                            .build(),
                    )
                    .build(),
            )
            .save(::save)
            .build()

    fun save() {
        configPath.writeText(Json.encodeToString(this))
    }

    fun createScreen(parent: Screen) = config.generateScreen(parent)
}
