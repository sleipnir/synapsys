package io.creativesource.synapsys.core.internals.serialization

import io.creativesource.synapsys.core.internals.MessageSerializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlinx.serialization.protobuf.ProtoBuf

@OptIn(ExperimentalSerializationApi::class)
class KotlinxMessageSerializer : MessageSerializer {

    override fun <T> serialize(obj: T): ByteArray {
        val serializer = serializer(obj!!::class.java) as KSerializer<T>
        return ProtoBuf.encodeToByteArray(serializer, obj)
    }

    override fun <T> deserialize(bytes: ByteArray, clazz: Class<T>): T {
        val serializer = serializer(clazz::class.java) as KSerializer<T>
        return ProtoBuf.decodeFromByteArray(serializer, bytes)
    }
}
