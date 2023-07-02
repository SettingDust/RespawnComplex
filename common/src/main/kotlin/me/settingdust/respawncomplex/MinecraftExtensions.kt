package me.settingdust.respawncomplex

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.settingdust.respawncomplex.serialization.BlockPosAsLongSerializer
import me.settingdust.respawncomplex.serialization.LevelSerializer
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel

@Serializable
data class Location(@Contextual val level: ServerLevel, @Contextual val pos: BlockPos) {
    class Serializer(server: MinecraftServer) : KSerializer<Location> {
        private val levelSerializer = LevelSerializer(server)
        override val descriptor = buildClassSerialDescriptor("Location") {
            element("level", levelSerializer.descriptor)
            element("pos", BlockPosAsLongSerializer.descriptor)
        }

        override fun deserialize(decoder: Decoder) = decoder.beginStructure(descriptor).run {
            var level: ServerLevel? = null
            var pos: BlockPos? = null
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    0 -> level = decodeSerializableElement(descriptor, i, levelSerializer)
                    1 -> pos = decodeSerializableElement(descriptor, i, BlockPosAsLongSerializer)
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> error("Unexpected index: $i")
                }
            }
            endStructure(descriptor)
            Location(level!!, pos!!)
        }

        override fun serialize(encoder: Encoder, value: Location) {
            encoder.beginStructure(descriptor).run {
                encodeSerializableElement(descriptor, 0, levelSerializer, value.level)
                encodeSerializableElement(descriptor, 1, BlockPosAsLongSerializer, value.pos)
                endStructure(descriptor)
            }
        }
    }
}
