package me.settingdust.respawncomplex.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.Vec3i
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel

class LevelSerializer(private val server: MinecraftServer) : KSerializer<ServerLevel> {
    override val descriptor = PrimitiveSerialDescriptor(ServerLevel::class.simpleName!!, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) =
        server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation(decoder.decodeString())))!!

    override fun serialize(encoder: Encoder, value: ServerLevel) =
        encoder.encodeString(value.dimension().location().toString())
}

object BlockPosAsLongSerializer : KSerializer<BlockPos> {
    override val descriptor = PrimitiveSerialDescriptor(BlockPos::class.simpleName!!, PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder) = BlockPos.of(decoder.decodeLong())

    override fun serialize(encoder: Encoder, value: BlockPos) = encoder.encodeLong(value.asLong())
}

object Vec3iSerializer : KSerializer<Vec3i> {
    override val descriptor = buildClassSerialDescriptor(Vec3i::class.simpleName!!) {
        element<Int>("x")
        element<Int>("y")
        element<Int>("z")
    }

    override fun deserialize(decoder: Decoder): Vec3i {
        val compositeDecoder = decoder.beginStructure(descriptor)
        var x = 0
        var y = 0
        var z = 0
        loop@ while (true) {
            when (val index = compositeDecoder.decodeElementIndex(descriptor)) {
                0 -> x = compositeDecoder.decodeIntElement(descriptor, index)
                1 -> y = compositeDecoder.decodeIntElement(descriptor, index)
                2 -> z = compositeDecoder.decodeIntElement(descriptor, index)
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw IllegalArgumentException("Unexpected index: $index")
            }
        }
        compositeDecoder.endStructure(descriptor)
        return Vec3i(x, y, z)
    }

    override fun serialize(encoder: Encoder, value: Vec3i) {
        encoder.beginStructure(descriptor).apply {
            encodeIntElement(descriptor, 0, value.x)
            encodeIntElement(descriptor, 1, value.y)
            encodeIntElement(descriptor, 2, value.z)
        }.endStructure(descriptor)
    }
}
