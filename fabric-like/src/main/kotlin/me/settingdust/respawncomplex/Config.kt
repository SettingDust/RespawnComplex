package me.settingdust.respawncomplex

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import dev.isxander.yacl.api.ConfigCategory
import dev.isxander.yacl.api.NameableEnum
import dev.isxander.yacl.api.Option
import dev.isxander.yacl.api.YetAnotherConfigLib
import dev.isxander.yacl.gui.controllers.BooleanController
import dev.isxander.yacl.gui.controllers.cycling.EnumController
import dev.isxander.yacl.gui.controllers.slider.IntegerSliderController
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

    private val config: YetAnotherConfigLib
        get() = YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("respawn_complex.name"))
            .category(
                ConfigCategory.createBuilder()
                    .name(Component.translatable("respawn_complex.config.category.activation"))
                    .option(
                        Option.createBuilder(Boolean::class.java)
                            .name(Component.translatable("respawn_complex.option.enable_activation"))
                            .tooltip(Component.translatable("respawn_complex.option.enable_activation.tooltip"))
                            .binding(true, ::_enableActivation) { _enableActivation = it }
                            .controller(::BooleanController)
                            .build(),
                    )
                    .option(
                        Option.createBuilder(ActivateMethod::class.java)
                            .name(Component.translatable("respawn_complex.option.activate_method"))
                            .tooltip(
                                Component.translatable(
                                    "respawn_complex.option.activate_method.tooltip",
                                    ActivateMethod.INTERACT.displayName,
                                    ActivateMethod.MOVING.displayName,
                                ),
                            )
                            .binding(ActivateMethod.INTERACT, ::_activateMethod) { _activateMethod = it }
                            .controller(::EnumController)
                            .build(),
                    )
                    .option(
                        Option.createBuilder(Boolean::class.java)
                            .name(Component.translatable("respawn_complex.option.send_activation_message"))
                            .binding(true, ::_sendActivationMessage) { _sendActivationMessage = it }
                            .controller(::BooleanController)
                            .build(),
                    )
                    .option(
                        Option.createBuilder(Int::class.java)
                            .name(Component.translatable("respawn_complex.option.activation_range"))
                            .tooltip(Component.translatable("respawn_complex.option.activation_range.tooltip"))
                            .binding(4, ::_activationRange) { _activationRange = it }
                            .controller { IntegerSliderController(it, 0, 32, 1) }
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
